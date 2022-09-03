package swingIO;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import RandomAccessFileV.*;

public class dataDescriptor extends JComponent implements IOEventListener, AdjustmentListener, MouseWheelListener, MouseListener
{
  //The file system stream reference that will be used.

  private RandomAccessFileV IOStream;

  //The current data descriptor.

  private Descriptor data;

  //The data type inspector.

  private dataInspector di;

  //Scroll bar position in data.

  private JScrollBar ScrollBar;

  //The default system font.

  private static FontMetrics ft;
  private static int strSizec1 = 0, strSizec2 = 0, strSizec3 = 0;
  private static int strEnd = 0;

  //Allows us to switch, and set data models.

  private boolean set = false, cset = false;

  //Selected row number.

  private int selectedRow = -1;

  //Used to display the data type.

  private final static String[] DType = new String[]{ "8Bit", "", "Int8", "", "UInt8", "", "Int16", "LInt16", "UInt16", "LUInt16", "Int32", "LInt32", "UInt32", "LUInt32", "Int64", "LInt64", "UInt64", "LUInt64", "Float32", "LFloat32", "Float64", "LFloat64", "Char8", "", "Char16", "LChar16", "String8", "", "String16", "LString16", "Other", "", "Array" };

  //The data model is for displaying data decoded as we scroll through a binary file.

  private class dataModel extends JComponent
  {
    public dataModel() { }

    //Decode and draw the binary data stylized at current position in scroll bar and memory.

    public void paintComponent( Graphics g )
    {
      //Initialize the system default font and metrics.

      if( ft == null )
      {
        ft = g.getFontMetrics(); strEnd = ft.stringWidth("...");
      
        strSizec1 = ft.stringWidth("Use") >> 1; strSizec2 = ft.stringWidth("Raw Data") >> 1; strSizec3 = ft.stringWidth("Data Type") >> 1;
      }

      int width = super.getWidth(), Cols = width / 3, Rows = getHeight() >> 4;

      //The first row explains what each column is.

      g.setColor( new Color( 238, 238, 238 ) ); g.fillRect(0,0,width,16);

      //The Number of rows that will fit on screen.

      int minRows = Math.min( data.data.length, Rows ); ScrollBar.setVisibleAmount(Rows);

      g.setColor(Color.white); g.fillRect( 0, 16, width, minRows << 4 );

      //Draw the column lines.

      g.setColor(Color.black);

      g.drawLine(0, 0, 0, (minRows+1) << 4); g.drawLine(Cols, 0, Cols, (minRows+1) << 4 ); g.drawLine(Cols << 1, 0, Cols << 1, (minRows+1) << 4);
    
      g.drawLine(0, 16, width, 16);

      //Column names.

      int HCol = Cols >> 1; g.drawString("Use", HCol - strSizec1, 13); HCol += Cols; g.drawString("Raw Data", HCol - strSizec2, 13); HCol += Cols; g.drawString("Data Type", HCol - strSizec3, 13);

      //The current start and end row in the data by scroll bar position

      int curRow = ScrollBar.getValue(), endRow = Math.min( curRow + minRows, data.rows );

      //Number of bytes needed to fill in columns by data types.

      byte[] Data = new byte[data.bytes(curRow,endRow)];

      //Data relative position.

      int rn = data.relPos[curRow];

      //Seek to data and read it.

      IOStream.Events = false; try
      {
        if( !data.virtual ) { IOStream.seek(data.pos + rn); IOStream.read(Data); }
        else { IOStream.seekV(data.pos + rn); IOStream.readV(Data); }
      }
      catch( java.io.IOException e ){} IOStream.Events = true;

      //Fill in the columns based on the current position of the scroll bar.

      for( int i = curRow, posY = 32; i < endRow; posY += 16, i++ )
      {
        if( i == selectedRow ){ g.setColor( new Color( 57, 105, 138, 128 ) ); g.fillRect(0, posY - 16, width, 16); g.setColor(Color.BLACK); }

        drawString( g, data.des[i], 2, posY - 3, Cols );

        drawString( g, Data, Cols + 2, posY - 3, data.relPos[i] - rn, data.relPos[i + 1] - rn , Cols );

        g.drawString( DType[data.data[i]], ( Cols << 1 ) + 2, posY - 3 );

        g.drawLine(0, posY, width, posY);
      }
    }

    //For the time being it is easier to separate this from the main rendering function.
    //It draws as many characters as posable in the given space of a column.

    private void drawString( Graphics g, String str, int x, int y, int width)
    {
      //When drawing text we must make sure it fits the col otherwise we put "...".

      int strLen = 4, i2 = 0;

      boolean fits = true; for(int len = str.length(); i2 < len && fits; i2++ )
      {
        strLen += ft.charWidth(str.charAt(i2)); if( strLen > width && i2 > 0 )
        {
          fits = false; strLen += strEnd; while( strLen > width && i2 > 1 ) { strLen -= ft.charWidth(str.charAt(i2--)); }
        }
      }

      g.drawString( fits ? str : str.substring(0, i2) + "..." , x, y );
    }

    //For the time being it is easier to separate this from the main rendering function.
    //It draws as many characters as posable in the given space of a column.
    //Note we can combine these methods if we work with strings as char array.

    private void drawString( Graphics g, byte[] d, int x, int y, int b1, int b2, int width)
    {
      //When drawing text we must make sure it fits the col otherwise we put "...".

      int strLen = 4, ln = 0, b = 0; b2 = Math.min( b2, d.length );

      char c1 = ' ', c2 = ' '; String str = "";

      boolean fits = true; while( b1 < b2 && fits )
      {
        b = ( d[b1] & 0xF0 ) >> 4; c1 = (char)(( b > 9 ) ? b + 0x37 : b + 0x30); b = d[b1] & 0xF; c2 = (char)(( b > 9 ) ? b + 0x37 : b + 0x30);

        str += c1; str += c2; strLen += ft.charWidth(c1) + ft.charWidth(c2); if( strLen > width )
        {
          fits = false; strLen += strEnd; ln = str.length() - 1; while( strLen > width && ln > 1 ) { strLen -= ft.charWidth(str.charAt(ln--)); }
        }

        str += ' '; strLen += ft.charWidth(' '); b1 += 1;
      }

      g.drawString( fits ? str : str.substring(0, ln) + "..." , x, y );
    }
  }

  //The core model is for mapped addresses and data in a machine code binary.

  private class coreModel extends JComponent
  {
    public coreModel() { }

    public void paintComponent( Graphics g )
    {

    }
  }

  //The main graphics components.

  private JComponent dModel = new dataModel(), cModel = new coreModel();

  //Create Data descriptor table.

  public dataDescriptor( RandomAccessFileV f, dataInspector d )
  {
    di = d; IOStream = f;

    ScrollBar = new JScrollBar(JScrollBar.VERTICAL);

    ScrollBar.setUnitIncrement( 1 ); ScrollBar.setBlockIncrement( 1 );

    ScrollBar.addAdjustmentListener(this); super.setLayout(new BorderLayout());

    super.add(dModel); super.add( ScrollBar, BorderLayout.EAST );

    super.addMouseListener(this); super.addMouseWheelListener(this); IOStream.addIOEventListener( this );
  }

  //Set the data model.

  public void setDescriptor( Descriptor d )
  {
    data = d; selectedRow = -1;
    
    ScrollBar.setMaximum(data.rows + 1); ScrollBar.setValue(0); d.Event.accept( -1 );
    
    repaint();
  }

  //Set the core data model. Allows us to navigate binary function calls and data.

  public void setDescriptor( core.Core d )
  {
    
  }

  //Main use is for setting a blank data model.

  public void clear() { data = new Descriptor(); repaint(); }

  //IO target change.

  public void setTarget( RandomAccessFileV f ) { IOStream = f; if( isVisible() ) { IOStream.addIOEventListener( this ); } }

  //Disable events when component is not visible.

  @Override public void setVisible( boolean v )
  {
    if( v && !isVisible() ) { IOStream.addIOEventListener(this); } else if ( !v && isVisible() ) { IOStream.removeIOEventListener(this); }
    super.setVisible( v );
  }
  
  public void mouseWheelMoved( MouseWheelEvent e ) { ScrollBar.setValue( ScrollBar.getValue() + ( e.getUnitsToScroll() ) ); repaint(); }

  public void adjustmentValueChanged(AdjustmentEvent e) { if(e.getValueIsAdjusting()){ repaint(); } }

  public void mousePressed( MouseEvent e )
  {
    selectedRow = Math.min( ScrollBar.getValue() + ( ( e.getY() >> 4 ) ), data.rows ) - 1; if( selectedRow < 0 ) { return; }

    try { if( !data.virtual ) { IOStream.seek(data.pos + data.relPos[selectedRow]); } else { IOStream.seekV(data.pos + data.relPos[selectedRow]); } } catch( java.io.IOException er ) { }
    
    di.setType( data.data[selectedRow] >> 1, (data.data[selectedRow] & 1) == 1 ); data.Event.accept( selectedRow ); repaint();
  }
  
  public void mouseExited( MouseEvent e ) { }
  
  public void mouseEntered( MouseEvent e ) { }
  
  public void mouseReleased( MouseEvent e ) { }

  public void mouseClicked( MouseEvent e ) { }

  public void onSeek( IOEvent e ) { }

  public void onRead( IOEvent e ) { }

  //Update the data descriptor if data changed because of an write operation from somewhere else.

  public void onWrite( IOEvent e )
  {
    if( !data.virtual && e.SPos() < (data.pos + data.length()) && e.EPos() >= data.pos ) { repaint(); }
    else if( e.SPosV() < (data.pos + data.length()) && e.EPosV() >= data.pos ) { repaint(); }
  }
}