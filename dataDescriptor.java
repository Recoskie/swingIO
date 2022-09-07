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

  //The core that we wish to display and interact with. For disassembly and data lookup.

  private core.Core core;

  //The data type inspector.

  private dataInspector di;

  //Scroll bar position in data.

  private JScrollBar ScrollBar;

  //The default system font.

  private static final FontMetrics ft = new JPanel().getFontMetrics( new JPanel().getFont() );

  //Data model columns names size.

  private static int mStrSize1 = ft.stringWidth("Use") >> 1, mStrSize2 = ft.stringWidth("Raw Data") >> 1, mStrSize3 = ft.stringWidth("Data Type") >> 1;

  //Core model columns names size.

  private static int cStrSize1 = ft.stringWidth("Operation") >> 1, cStrSize2 = ft.stringWidth("Address") >> 1;
  private static int strEnd = ft.stringWidth("..."); //The "..." size for when text does not fit in column.
  private static int minWidth = 0; //minimum width required to display the component properly.
  
  //Only update component constants as needed on resize.
  
  private int width = 0, height = 0, compX = 0, compY = 0;
  private int cols2 = 0, cols3 = 0, visibleRows = 0;

  //Allows us to switch, and set data models.

  private boolean coreMode = false;

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
      //The first row explains what each column is.

      g.setColor( new Color( 238, 238, 238 ) ); g.fillRect(0,0,width,16);

      //The Number of rows that will fit on screen.

      int minRows = Math.min( data.rows, visibleRows ); ScrollBar.setVisibleAmount(visibleRows);

      g.setColor(Color.white); g.fillRect( 0, 16, width, minRows << 4 );

      //Draw the column lines.

      g.setColor(Color.black);

      g.drawLine(0, 0, 0, (minRows+1) << 4); g.drawLine(cols3, 0, cols3, (minRows+1) << 4 ); g.drawLine(cols3 << 1, 0, cols3 << 1, (minRows+1) << 4);
    
      g.drawLine(0, 16, width, 16);

      //Column names.

      int HCol = cols3 >> 1; g.drawString("Use", HCol - mStrSize1, 13); HCol += cols3; g.drawString("Raw Data", HCol - mStrSize2, 13); HCol += cols3; g.drawString("Data Type", HCol - mStrSize3, 13);

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
      catch( java.io.IOException e ) { } IOStream.Events = true;

      //Fill in the columns based on the current position of the scroll bar.

      for( int i = curRow, posY = 32; i < endRow; posY += 16, i++ )
      {
        if( i == selectedRow ){ g.setColor( new Color( 57, 105, 138, 128 ) ); g.fillRect(0, posY - 16, width, 16); g.setColor(Color.BLACK); }

        drawString( g, data.des[i], 2, posY - 3, cols3 );

        drawString( g, Data, cols3 + 2, posY - 3, data.relPos[i] - rn, data.relPos[i + 1] - rn , cols3 );

        g.drawString( DType[data.data[i]], ( cols3 << 1 ) + 2, posY - 3 );

        g.drawLine(0, posY, width, posY);
      }
    }

    //Draws as many characters as posable in the given space of a column.

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
      //The first row explains what each column is.

      int addresses = ScrollBar.getMaximum();

      g.setColor( new Color( 238, 238, 238 ) ); g.fillRect(0,0,width,16);

      //The Number of rows that will fit on screen.

      int minRows = Math.min( addresses, visibleRows ); ScrollBar.setVisibleAmount(minRows);

      g.setColor(Color.white); g.fillRect( 0, 16, width, minRows << 4 );

      //Draw the column lines.

      g.setColor(Color.black); g.drawLine(cols2, 0, cols2, (minRows+1) << 4); g.drawLine(0, 16, width, 16);

      //Column names.

      int HCol = cols2 >> 1; g.drawString("Operation", HCol - cStrSize1, 13); HCol += cols2; g.drawString("Address", HCol - cStrSize2, 13);

      //The current start and end row in the data by scroll bar position

      int curRow = ScrollBar.getValue(), endRow = Math.min( curRow + minRows, addresses );

      //Display the addresses and operations that can be carried out.

      for( int i = curRow, posY = 32; i < endRow; posY += 16, i++ )
      {
        if( i == selectedRow ){ g.setColor( new Color( 57, 105, 138, 128 ) ); g.fillRect(0, posY - 16, width, 16); g.setColor(Color.BLACK); }

        //Each operation is sorted into a list as the core engine reads the binary.

        int row = i; if( row < ( core.Linear.size() >> 1 ) )
        {
          g.drawString( "LDisassemble", 2, posY - 3 );
          g.drawString( "0x" + String.format( "%1$016X", core.Linear.get( row << 1 ) ), cols2 + 2, posY - 3 );
        }
        else if( ( row -= ( core.Linear.size() >> 1 ) ) < core.Crawl.size() )
        {
          g.drawString( "Disassemble", 2, posY - 3 );
          g.drawString( "0x" + String.format( "%1$016X", core.Crawl.get( row ) ), cols2 + 2, posY - 3 );
        }
        else
        {
          row -= core.Crawl.size();
          g.drawString( "Data", 2, posY - 3 );
          g.drawString( "0x" + String.format( "%1$016X", core.data_off.get( row << 1 ) ), cols2 + 2, posY - 3 );
        }

        g.drawLine(0, posY, width, posY);
      }
    }
  }

  //The main graphics components.

  private JComponent dModel = new dataModel(), cModel = new coreModel();

  //Create Data descriptor table.

  public dataDescriptor( RandomAccessFileV f, dataInspector d )
  {
    di = d; IOStream = f; ScrollBar = new JScrollBar(JScrollBar.VERTICAL);

    ScrollBar.setUnitIncrement( 1 ); ScrollBar.setBlockIncrement( 1 );

    ScrollBar.addAdjustmentListener(this); super.setLayout(new BorderLayout());

    super.add(dModel); super.add( ScrollBar, BorderLayout.EAST );

    super.addMouseListener(this); super.addMouseWheelListener(this); IOStream.addIOEventListener( this );

    coreMode = false;

    //longest char width in hex.

    char[] h = new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    int cur = 0; for( int i = 0, t = ft.charWidth(h[0]); i < h.length; t=ft.charWidth(h[i++]) )
    {
      if( cur < t ) { cur = t; }
    }
  
    //Component minimum width is the minium column size for an 64 bit address.
    
    minWidth = (ft.stringWidth("0x") + (cur << 4) + 2) << 1;
  }

  //Set the data model.

  public void setDescriptor( Descriptor d )
  {
    if( coreMode ){ super.remove(0); super.add(dModel,0); IOStream.addIOEventListener(this); coreMode = false; revalidate(); }

    data = d; selectedRow = -1;
    
    ScrollBar.setMaximum(data.rows + 1); ScrollBar.setValue(0); d.Event.accept( -1 );
    
    repaint();
  }

  //Set the core data model. Allows us to navigate binary function calls and data.

  public void setDescriptor( core.Core d )
  {
    if( !coreMode ){ super.remove(0); super.add(cModel,0); IOStream.removeIOEventListener(this); coreMode = true; revalidate(); }

    core = d; selectedRow = -1;
    
    ScrollBar.setMaximum(core.Crawl.size() + ( core.Linear.size() >> 1 ) + ( core.data_off.size() >> 1 )); ScrollBar.setValue(0); repaint();
  }

  //Main use is for setting a blank data model.

  public void clear()
  {
    if( coreMode ){ super.remove(0); super.add(dModel,0); IOStream.addIOEventListener(this); coreMode = false; revalidate(); }

    data = new Descriptor(); ScrollBar.setMaximum(data.rows + 1); ScrollBar.setValue(0); repaint();
  }

  //IO target change.

  public void setTarget( RandomAccessFileV f ) { IOStream = f; if( isVisible() ) { IOStream.addIOEventListener( this ); } }

  //Disable events when component is not visible.

  @Override public void setVisible( boolean v )
  {
    if( !coreMode )
    {
      if( v && !isVisible() ) { IOStream.addIOEventListener(this); } else if ( !v && isVisible() ) { IOStream.removeIOEventListener(this); }
    }

    super.setVisible( v );
  }

  @Override public Dimension getMinimumSize()
  {
    return( new Dimension( minWidth, 48 ) );
  }

  @Override public Dimension getPreferredSize()
  {
    return( new Dimension( minWidth, Math.max( super.getHeight(), 48 ) ) );
  }
  
  public void mouseWheelMoved( MouseWheelEvent e ) { ScrollBar.setValue( ScrollBar.getValue() + ( e.getUnitsToScroll() ) ); repaint(); }

  public void adjustmentValueChanged(AdjustmentEvent e) { if(e.getValueIsAdjusting()){ repaint(); } }

  public void mousePressed( MouseEvent e )
  {
    selectedRow = ScrollBar.getValue() + ( e.getY() >> 4 ); if( selectedRow < 1 ) { return; }

    if( !coreMode )
    {
      selectedRow = Math.min( selectedRow, data.rows ) - 1;

      try { if( !data.virtual ) { IOStream.seek(data.pos + data.relPos[selectedRow]); } else { IOStream.seekV(data.pos + data.relPos[selectedRow]); } } catch( java.io.IOException er ) { }
    
      di.setType( data.data[selectedRow] >> 1, (data.data[selectedRow] & 1) == 1 ); data.Event.accept( selectedRow );
    }
    else
    {
      selectedRow = Math.min( selectedRow, ScrollBar.getMaximum() ) - 1;

      if( selectedRow < ( core.Linear.size() >> 1 ) ) { core.disLoc( selectedRow, false ); }
      else if( ( selectedRow -= ( core.Linear.size() >> 1 ) ) < core.Crawl.size() ) { core.disLoc( selectedRow, true ); }
      else
      {
        selectedRow -= core.Crawl.size(); selectedRow = selectedRow << 1;

        try { core.setLoc( core.data_off.get( selectedRow ) ); } catch( Exception er ) { }

        di.setOther( core.data_off.get( selectedRow + 1 ).intValue() );
      }

      ScrollBar.setMaximum(core.Crawl.size() + ( core.Linear.size() >> 1 ) + ( core.data_off.size() >> 1 ));

      selectedRow = Math.min( ScrollBar.getValue() + ( e.getY() >> 4 ), ScrollBar.getMaximum() ) - 1;
    }
    
    repaint();
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

  //Only update component as needed.

  @Override public void setBounds( int x, int y, int w, int h )
  {
    if( width != w || height != h )
    {
      width = w; height = h;

      cols2 = width >> 1; cols3 = width / 3; visibleRows = height >> 4;

      super.setBounds(x, y, w, h);
    }
    else if( compX != x || compY != y )
    {
      super.setBounds(x, y, w, h);
    }
  }
}