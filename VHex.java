package swingIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import RandomAccessFileV.*;

public class VHex extends JComponent implements IOEventListener, MouseWheelListener, MouseMotionListener, MouseListener, KeyListener, Runnable
{
  //The file system stream reference that will be used.

  private RandomAccessFileV IOStream;

  //Number of rows in draw space.

  private int Rows = 0;
  
  //Selection Color.
  
  private Color SelectC = new Color(57, 105, 138, 128);
  
  //Byte buffer between io stream. Updated based on number of rows that can be displayed.

  private byte[] data = new byte[0];
  private boolean[] udata = new boolean[0]; //Bytes that could not be read.
  
  //A modified scroll bar for VHex. Allows for a much larger scroll area of very big data.

  private LongScrollBar ScrollBar;

  //Virtual mode, or offset mode.

  private boolean Virtual = false;

  //Character width varies based on length of string even though it is Monospaced. This is because characters can be spaced apart as a fraction like 9+(1/3).
  //Could divide a 17 in length string by 17 to get the fractional char width as float. However integers are faster, and do not need rounding.

  private int[] charWidth = new int[18];

  //Height is always the same. Even if font is not Monospaced.

  private int lineHeight = 0;

  //Basic graphics.

  private int scrollBarSize = 0; //Width of scroll bar.
  private int cell = 0; //Size of each hex cell.
  private int addcol = 0; //The address column width.
  private int hexend = 0; //End of hex columns.
  private int textcol = 0; //Text column start.
  private int addc = 0; //Center position of Address col string.
  private int textc = 0; //Center position of text string.
  private int endw = 0; //End of component.
  private int x = 0, y = 0; //X, and Y.
  private long t = 0; //temporary value.

  //Font is loaded on Initialize.

  private Font font;

  //Hex editor offset.

  private long offset = 0;

  //Element based selection.

  private boolean elSelection = false;

  //Start and end index of selected bytes.

  private long sel = 0, sele = 0;
  private long elPos = 0, elLen = 0;

  //slide animation.

  private byte slide = 0;

  //The address column.

  private String s = "Offset (h)";

  //Edit mode.

  private boolean emode = false, etext = false, drawc = false, wr = false;
  private long ecellX = 0, ecellY = 0;

  //Text pane.

  private boolean text = false;

  //Update data.

  private int rd = 0, pos = 0, end = 0;

  public void updateData()
  {
    //Read data offset position.

    offset &= 0xFFFFFFFFFFFFFFF0L;
    
    rd = 0; pos = 0; end = 0; udata = new boolean[data.length];

    try
    {
      //If offset mode use offset seek.

      if (!Virtual)
      {
        IOStream.Events = false;

        //backup current address. 
          
        t = IOStream.getFilePointer();
          
        IOStream.seek( offset );

        rd = IOStream.read( data ); rd = rd < 0 ? 0 : rd;

        for( int i = rd; i < data.length; i++ ) { udata[i] = true; }

        //restore address.

        IOStream.seek( t );
          
        IOStream.Events = true;
      }

      //If Virtual use Virtual map seek.

      else
      {
        IOStream.Events = false;

        //backup current address.          

        t = IOStream.getVirtualPointer();

        while( pos < data.length )
        {
          IOStream.seekV( offset + pos );

          rd = IOStream.readV( data, pos, data.length - pos ); rd = rd < 0 ? 0 : rd; pos += rd;

          end = (int)IOStream.endV();

          if( ( end + pos ) > data.length || end <= 0 ) { end = data.length - pos; }

          for( int i = 0; i < end; pos++, i++ ) { udata[pos] = true; }
        }

        //restore address.

        IOStream.seekV( t );
          
        IOStream.Events = true;
      }
    }
    catch ( Exception er )
    {
      for( int i = rd; i < data.length; i++ ) { udata[i] = true; }
    }

    t = 0;
  }
  
  //Modified scrollbar class to Handel the full 64 bit address space.
  //Virtual space is treated as unsigned. Except physical file positions.
  
  private class LongScrollBar extends JScrollBar
  {
    private long End = 0, VisibleEnd = 0;

    private final int RelUp = 0x70000000, RelDown = 0x10000000;
    
    private int ov = 0, rel = 0;

    private long oldOffset = 0;

    private boolean extend = false;

    private VHex comp;
    
    public LongScrollBar(int orientation, int value, int visible, int minimum, long maximum, VHex c )
    {
      super( orientation, value, visible, minimum, Long.compareUnsigned( maximum, 0x7FFFFFF0 ) > 0 ? 0x7FFFFFF0 : (int) ( ( maximum + 15 ) & 0x7FFFFFF0 ) );
      
      End = maximum; extend = Long.compareUnsigned( maximum, 0x7FFFFFF0 ) > 0;
      
      if( Long.compareUnsigned( visible, End ) < 0 ) { VisibleEnd = ( End - visible ) & 0x7FFFFFFFFFFFFFF0L; }
      
      comp = c;
    }
    
    @Override public void setValue( int v )
    {
      //Relatively scroll really large files that are too big for scroll bar.

      if( extend )
      {
        rel = v - ov; ov = v; offset += rel; oldOffset = offset;

        //Limit offset within the disk size, or file size.

        if( !Virtual ) { if( offset < 0 ) { offset = 0; } else if( Long.compareUnsigned( offset, VisibleEnd ) > 0 ) { offset = VisibleEnd; } }

        //Adjust scroll bar.

        if( rel < 0 && ov < RelDown )
        {
          //Set Scroll pos fixed until start of data.

          if( offset > RelDown ) { ov = RelDown; }

          //Remaining data to start.
        
          else { ov = (int)offset & 0x7FFFFFF0; }
        }

        if( rel > 0 && ov > RelUp )
        {
          //Set Scroll pos fixed until remaining data.

          if( ( VisibleEnd - offset ) > RelDown ){ ov = RelUp; }
        
          //Remaining data to end.
        
          else { ov = (int)( 0x7FFFFFF0 - ( End - offset ) ); }
        }

        v = ov;
      }
      else
      {
        offset = v; ov = v;
        
        //Limit offset within the disk size, or file size.

        if( offset < 0 ) { offset = 0; } else if( offset > VisibleEnd ) { offset = VisibleEnd; }
      }

      //Update scroll bar relative positioning.
      
      super.setValue( v ); updateData(); comp.repaint();
    }
    
    @Override public void setVisibleAmount( int v )
    {
      if( Long.compareUnsigned( v, End ) < 0 ) { VisibleEnd = ( ( extend ? End : End + 15 ) - v ) & 0x7FFFFFFFFFFFFFF0L; } else { VisibleEnd = 0; }
      
      super.setVisibleAmount( v );
      
      if( Long.compareUnsigned( offset, VisibleEnd ) > 0 ) { setValue( VisibleEnd ); }
    }
    
    public void setValue( long v )
    {
      offset = v & 0xFFFFFFFFFFFFFFF0L;

      rel = (int)(v - oldOffset); v = ov + rel; ov = (int)v; oldOffset = offset;

      if( extend )
      {
        if( !Virtual ) { if( offset < 0 ) { offset = 0; } else if( Long.compareUnsigned( offset, VisibleEnd ) > 0 ) { offset = VisibleEnd; } }

        //Adjust scroll bar.

        if( rel < 0 && ov < RelDown )
        {
          //Set Scroll pos fixed until start of data.

          if( offset > RelDown ) { ov = RelDown; }

          //Remaining data to start.
        
          else { ov = (int)offset & 0x7FFFFFF0; }
        }

        if( rel > 0 && ov > RelUp )
        {
          //Set Scroll pos fixed until remaining data.

          if( ( VisibleEnd - offset ) > RelDown ){ ov = RelUp; }
        
          //Remaining data to end.
        
          else { ov = (int)( 0x7FFFFFF0 - ( End - offset ) ); }
        }

        v = ov;
      }

      super.setValue( (int) v );
    }

    public void setMaximum( long v )
    {
      End = v; extend = Long.compareUnsigned( v, 0x7FFFFFF0 ) > 0;
      
      super.setMaximum( extend ? 0x7FFFFFF0 : (int) ( ( v + 15 ) & 0x7FFFFFF0 ) );
    }
  }

  //Add data type tool to hex editor.

  public VHex(RandomAccessFileV f, swingIO.dataInspector d, boolean mode) { this(f, mode); d.addEditor( this ); }

  //Add data type tool to hex editor. If no mode setting then assume offset mode.

  public VHex(RandomAccessFileV f, swingIO.dataInspector d) { this(f, false); d.addEditor( this ); }
  
  //If no mode setting then assume offset mode.

  public VHex(RandomAccessFileV f) { this(f, false); }

  //Initialize the hex UI component. With file system stream.

  public VHex(RandomAccessFileV f, boolean mode)
  {
    //Register this component to update on IO system calls.
    
    f.addIOEventListener( this ); Virtual = mode; if( mode ) { s = "Virtual Address (h)"; }

    //Reference the file stream.

    IOStream = f;

    //Setup Scroll bar system.
      
    try{ ScrollBar = new LongScrollBar(JScrollBar.VERTICAL, 16, 0, 0, Virtual ? 0xFFFFFFFFFFFFFFFFL : IOStream.length() + 32, this ); } catch( java.io.IOException e ) { }
    
    ScrollBar.setUnitIncrement( 16 ); ScrollBar.setBlockIncrement( 16 );

    //Custom selection handling.

    super.addMouseListener(this); super.addMouseMotionListener(this);

    //Add scroll wheal handler.
    
    super.addMouseWheelListener(this);

    //Key listener for edit mode.

    super.addKeyListener(this);

    try
    {
      font = Font.createFont( Font.TRUETYPE_FONT, VHex.class.getResourceAsStream("Font/DOS.ttf") ).deriveFont( 16f );
    }
    catch( Exception er ) { font = new Font( "Monospaced", Font.BOLD, 16 ); }
  }

  //Disable events when component is not visible.

  @Override public void setVisible( boolean v )
  {
    if( v && !isVisible() ) { IOStream.addIOEventListener(this); } else if ( !v && isVisible() ) { IOStream.removeIOEventListener(this); }
    super.setVisible( v );
  }

  //Get selected byte index.

  public long selectPos() { return( Long.compareUnsigned( sel, sele ) <= 0 ? sel : sele ); }

  //Get last selected byte index.

  public long selectEnd() { return( Long.compareUnsigned( sel, sele ) >= 0 ? sel : sele ); }

  //Get selected element pos.

  public long selectEl() { return( elPos ); }

  //Set selected bytes.

  public void setSelected( long start, long end )
  {
    elSelection = false; SelectC = new Color( 57, 105, 138, 128 );
    
    if( !Virtual || IOStream.isMaped() ) { sel = start; sele = end; repaint(); }
  }

  public void setSelectedEnd( long end )
  {
    elSelection = false; SelectC = new Color( 57, 105, 138, 128 );
    
    if( !Virtual || IOStream.isMaped() ) { sele = end; repaint(); }
  }

  //Element selection. A group of bytes that create the data type.

  public void setSelectedEl( long pos, long len )
  {
    SelectC = new Color( 57, 105, 138, 128 );
    
    if( !Virtual || IOStream.isMaped() ) { elSelection = true; elPos = pos; elLen = len; repaint(); }
  }

  //set selection mode.

  public void setSelectMode( boolean m ) { elSelection = !m; }

  //Enable, or disable the text editor.

  public void enableText( boolean set )
  {
    text = set;

    if( text ) { endw = textcol + charWidth[16]; } else { endw = hexend; }

    super.removeAll(); super.add( Box.createRigidArea( new Dimension( endw, 0 ) ) ); super.add( ScrollBar );

    validate();
  }

  //Check if text is enabled.

  public boolean showText() { return( text ); }

  //Check if editing data.

  public boolean isEditing() { return( emode ); }

  //check if virtual mode.

  public boolean isVirtual() { return( Virtual ); }

  //Initialize the draw area and component size.

  private void init()
  {
    scrollBarSize = ((Integer)UIManager.get("ScrollBar.width")).intValue();

    java.awt.FontMetrics fm = super.getFontMetrics(font); lineHeight = fm.getHeight();

    //Get width, for different length strings.

    String sLen = ""; for( int i = 0; i < charWidth.length; sLen += " ", charWidth[ i++ ] = fm.stringWidth( sLen ) );

    //Cell size, and address column size.

    cell = charWidth[1] + 4;
      
    addcol = charWidth[17] + 4;

    hexend = addcol + ( cell << 4 );

    textcol = hexend + charWidth[0];

    if( text ) { endw = textcol + charWidth[16]; } else { endw = hexend; }

    //Center position of strings.

    addc = ( addcol >> 1 ) - ( fm.stringWidth(s) >> 1 ); textc = textcol + ( fm.stringWidth("Text") >> 1 ) + ( charWidth[4] );

    //Scroll bar at end of component.

    super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    super.add( Box.createRigidArea( new Dimension( endw, 0 ) ) ); super.add( ScrollBar );
  }

  //The component draw area.
  
  @Override public Dimension getMinimumSize()
  {
    if( charWidth[0] == 0 ) { init(); }

    return( new Dimension( endw + scrollBarSize, lineHeight << 3 ) );
  }

  @Override public Dimension getPreferredSize()
  {
    if( charWidth[0] == 0 ) { init(); }

    return( new Dimension( endw + scrollBarSize, Math.max( super.getHeight(), lineHeight << 3 ) ) );
  }
  
  //Render the component.

  public void paintComponent( Graphics g )
  {
    g.setFont( font ); if( charWidth[0] == 0 ) { init(); }

    //Adjust byte buffer on larger height.

    if( Rows != ( getHeight() / lineHeight ) ) { Rows = getHeight() / lineHeight; ScrollBar.setVisibleAmount( Rows << 4 ); data = java.util.Arrays.copyOf( data, Rows << 4 ); updateData(); }

    //Clear the component draw space.

    g.setColor( Color.white ); g.fillRect( 0, 0, endw, getHeight() );

    //Begin Graphics for hex editor component.

    g.setColor( new Color( 238, 238, 238 ) ); g.fillRect( 0, 0, endw, lineHeight );

    //Select character in edit mode.

    Selection( g );

    //Column description.

    g.setColor(Color.black);
    
    g.drawString( s, addc, lineHeight - 3 );

    g.fillRect(0,lineHeight,addcol,getHeight());

    if( text ) { g.drawString( "Text", textc, lineHeight - 3 ); }

    //Column cells.

    for(int i1 = addcol, index = 0; i1 < hexend; i1 += cell, index++ )
    {
      g.drawString( String.format( "%1$02X", index ), i1 + 1 , lineHeight - 3 ); g.drawLine( i1, lineHeight, i1, getHeight() );
    }

    g.drawLine( hexend, lineHeight, hexend, getHeight() );

    //Render data.

    byte[] b = new byte[ 16 ];
		
    for(int i1 = lineHeight, index = 0; index < data.length; i1 += lineHeight )
    {
      g.drawLine( addcol, i1, hexend, i1 );

      if( text )
      {
        for( int i2 = 0; i2 < 16; i2++ )
        {
          byte d = data[ index + i2 ];

          if( udata[ index + i2 ] ) { d = 0x3F; } else if( d == 9 || d == 10 || d == 13 ) { d = 0x20; }

          b[i2] = d;
        }

        g.drawBytes(b, 0, 16, textcol, i1 + lineHeight - 6 );
      }

      for(int i2 = addcol; i2 < hexend; i2 += cell, index++ )
      {
        g.drawString( udata[index] ? "??" : String.format( "%1$02X", data[index] ), i2 + 2, i1 + lineHeight - 3 );
      }
    }

    //Draw Cursor line.

    if( emode && drawc )
    {
      if( !etext )
      {
        //Cell alignment.

        x = ( ( ( (int)ecellX >> 1 ) + 1 ) * cell ) + addcol;

        //Character alignment.
      
        x -= ( ( (int)ecellX + 1 ) & 1 ) * ( cell >> 1 );

        //Border.
      
        x -= 2;

        y = ( (int)ecellY - (int)( offset >> 4 ) + 1 ) * lineHeight;
      }
      else
      {
        //Char alignment.

        x = charWidth[(int)ecellX >> 1] + textcol;

        //Border.
      
        x -= 2;

        y = ( (int)ecellY - (int)( offset >> 4 ) + 1 ) * lineHeight;
      }

      g.drawLine( x, y, x, y + lineHeight );
    }

    //Address offset.

    g.setColor(Color.white);

    for(int i1 = lineHeight, index = 0; index < data.length; i1 += lineHeight, index += 16 )
    {
      g.drawString( "0x" + String.format( "%1$016X", offset + index), 3, i1 + lineHeight - 3 );
    }
  }

  //Selection Handler.

  private void Selection( Graphics g )
  {
    g.setColor(SelectC);

    //Only select length bytes when element selection is enabled.

    if( elSelection ) { sel = elPos; sele = sel + elLen; }

    if( sel > sele ) { t = sele; sele = sel; sel = t; }
    
    x = (int)(sel - offset) >> 4; y = (int)(sele - offset) >> 4; y -= x; y += 1; x += 1;

    if( x < 1 ){ y += x - 1; x = 1; }

    if( x > 0 )
    {
      g.fillRect( addcol, x * lineHeight, hexend - addcol, y * lineHeight );

      if( text ) { g.fillRect( textcol, x * lineHeight, charWidth[15], y * lineHeight ); }
    }

    //Clear the start and end pos.

    g.setColor(Color.white);

    if( ( sel - offset ) > 0 )
    {
      g.fillRect( addcol, x * lineHeight, (int)(sel & 0xF) * cell, lineHeight );

      if( text && ( sel & 0xF ) != 0 ) { g.fillRect( textcol, x * lineHeight, charWidth[ (int)(sel & 0xF) - 1 ], lineHeight ); }
    }

    x += y - 1; y = ( (int)sele + 1 ) & 0xF;
    
    if( y > 0 )
    {
      g.fillRect( addcol + y * cell, x * lineHeight, ( 16 - y ) * cell, lineHeight );

      if( text ) { g.fillRect( textcol + charWidth[y-1], x * lineHeight, charWidth[16 - y], lineHeight ); }
    }

    if( emode && !etext )
    {
      g.fillRect( (int)( addcol + ( ecellX >> 1 ) * cell ), (int)( ( ecellY - ( offset >> 4 ) + 1 ) * lineHeight ), cell, lineHeight );
    }

    if( t != 0 ) { t = sele; sele = sel; sel = t; t = 0; }

    //Highlight current Hex digit in hex edit mode.

    if( emode && !etext )
    {
      g.setColor( new Color( 57, 105, 138, 128 ) );

      //Cell alignment.

      x = ( ( ( (int)ecellX >> 1 ) ) * cell ) + addcol;

      //Character alignment.
      
      x += ( ( (int)ecellX ) & 1 ) * ( cell >> 1 );

      y = ( (int)ecellY - (int)( offset >> 4 ) + 1 ) * lineHeight;

      g.fillRect( x, y, cell >> 1, lineHeight );
    }
  }
  
  //Adjust scroll bar on scroll wheal.
  
  public void mouseWheelMoved( MouseWheelEvent e ) { ScrollBar.setValue( ScrollBar.getValue() + ( e.getUnitsToScroll() << 4 ) ); }

  public void mouseMoved( MouseEvent e )
  {
    if(emode)
    {
      if( e.getX() > addcol && e.getX() < endw && e.getY() > lineHeight )
      {
        setCursor(new Cursor(Cursor.TEXT_CURSOR));
      }
      else
      {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  public void mouseDragged( MouseEvent e )
  {
    if( SwingUtilities.isLeftMouseButton( e ) )
    {
      if( !emode )
      {
        x = e.getX();
      
        if( text && x > hexend )
        {
          if( x > endw - ( charWidth[1] ) ) { x = endw - ( ( charWidth[1] ) + 1 ); }

          x -= textcol; if ( x < 0 ) { x = 0; }

          x = ( x / charWidth[0] ) & 0xF;
    
          y = ( e.getY() / lineHeight ) - 1; y <<= 4;
        }

        else
        {
          if( x > hexend ) { x = hexend - 1; }

          x -= addcol; if ( x < 0 ) { x = 0; }

          x = ( x / cell ) & 0xF;
    
          y = ( e.getY() / lineHeight ) - 1; y <<= 4;
        }

        if( ( x + y ) > ( ( Rows - 1 ) << 4 ) )
        {
          sele = x + ( ( Rows - 1 ) << 4 ) + offset;

          if( slide == 0 ) { new Thread(this).start(); }

          slide = 1;
        }
        else if( x + y < 16 )
        {
          sele = x + offset;
        
          if( slide == 0 ) { new Thread(this).start(); }

          slide = -1;
        }
      
        else { slide = 0; sele = x + y + offset; repaint(); }
      }
    }
  }

  public void mouseExited( MouseEvent e ) { }

  public void mouseEntered( MouseEvent e ) { }

  public void mouseReleased( MouseEvent e ) { slide = 0; }

  public void mousePressed( MouseEvent e )
  {
    if( SwingUtilities.isLeftMouseButton( e ) )
    {
      x = e.getX();
    
      if( text && x > hexend )
      {
        if( x > endw - ( charWidth[1] ) ) { x = endw - ( ( charWidth[1] ) + 1 ); }

        x -= textcol; if ( x < 0 ) { x = 0; }

        x = ( x / charWidth[0] ) & 0xF;
    
        y = ( e.getY() / lineHeight ) - 1; y <<= 4;
      }

      else
      {
        if( x > hexend ) { x = hexend - 1; }

        x -= addcol; if ( x < 0 ) { x = 0; }

        x = ( x / cell ) & 0xF;
    
        y = ( e.getY() / lineHeight ) - 1; y <<= 4;
      }

      if( emode ) { checkEdit(); }

      sel = x + y + offset;

      if( text && emode && e.getX() > textcol && e.getX() < endw && e.getY() > lineHeight )
      {
        ecellX = ( ( e.getX() - textcol ) / charWidth[0] ) << 1;

        ecellY = ( e.getY() / lineHeight ) + (int)( offset >> 4 ); ecellY -= 1;

        if( !udata[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] )
        {
          setCursor(new Cursor(Cursor.TEXT_CURSOR));

          if( !emode ) { emode = true; new Thread(this).start(); }

          etext = true;

          requestFocus(); repaint();
        }
      }
      else if( emode && e.getX() > addcol && e.getX() < hexend && e.getY() > lineHeight )
      {
        ecellX = ( e.getX() - addcol ) / cell;

        ecellX = (  ecellX << 1 ) + ( ( ( e.getX() - addcol ) % cell ) / ( cell >> 1 ) );

        ecellY = ( e.getY() / lineHeight ) + (int)( offset >> 4 ); ecellY -= 1;

        if( !udata[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] )
        {
          setCursor(new Cursor(Cursor.TEXT_CURSOR));

          if( !emode ) { emode = true; new Thread(this).start(); }

          etext = false;

          requestFocus(); repaint();
        }
      }

      try{ if( !Virtual ) { IOStream.seek( x + y + offset ); } else { IOStream.seekV( x + y + offset ); } } catch( Exception er ) { }
    }
  }

  //Begin hex edit mode.

  public void mouseClicked( MouseEvent e )
  {
    if( SwingUtilities.isLeftMouseButton( e ) )
    {
      if( !IOStream.readOnly && e.getClickCount() == 2 && e.getX() > textcol && e.getX() < endw && e.getY() > lineHeight )
      {
        ecellX = ( ( e.getX() - textcol ) / charWidth[0] ) << 1;

        ecellY = ( e.getY() / lineHeight ) + (int)( offset >> 4 ); ecellY -= 1;

        if( !udata[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] )
        {
          setCursor(new Cursor(Cursor.TEXT_CURSOR));

          if( !emode ) { emode = true; new Thread(this).start(); }

          if( !etext ) { checkEdit(); etext = true; }

          requestFocus(); repaint();
        }
      }
      else if( !IOStream.readOnly && e.getClickCount() == 2 && e.getX() > addcol && e.getX() < hexend && e.getY() > lineHeight )
      {
        ecellX = ( e.getX() - addcol ) / cell;

        ecellX = (  ecellX << 1 ) + ( ( ( e.getX() - addcol ) % cell ) / ( cell >> 1 ) );

        ecellY = ( e.getY() / lineHeight ) + (int)( offset >> 4 ); ecellY -= 1;

        if( !udata[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] )
        {
          setCursor(new Cursor(Cursor.TEXT_CURSOR));

          if( !emode ) { emode = true; new Thread(this).start(); }

          etext = false;

          requestFocus(); repaint();
        }
      }
    }
  }

   public void keyReleased( KeyEvent e ) { }

   //IO target change.

   public void setTarget( RandomAccessFileV f )
   {
     IOStream = f; if( isVisible() ) { IOStream.addIOEventListener( this ); }
     
     try { ScrollBar.setMaximum( Virtual ? 0xFFFFFFFFFFFFFFFFL : IOStream.length() + 32 ); ScrollBar.setVisibleAmount( Rows << 4 ); ScrollBar.setValue(0); } catch( Exception e ) { }
     
     offset = 0; sel = 0; sele = 0; SelectC = new Color(57, 105, 138, 128); updateData(); repaint();
   }

   //Writes modified byte before moving.

   private void checkEdit()
   {
     if( wr )
     {
       try { IOStream.write( data[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] ); wr = false; } catch( Exception er ) {}
     }
   }

   //Editable bytes.

   private boolean canEdit()
   {
     //Limit the user to only modify the bytes, of a data type element.

     long x = ( ecellX >> 1 ) + ( ecellY << 4 );

     if( elSelection )
     {
       if( x > ( elPos + elLen ) )
       {
         x = ( elPos + elLen );

         ecellX = ( ( x & 0xF ) << 1 ) + 1; ecellY = ( x & 0xFFFFFFFFFFFFFFF0L ) >> 4;

         return( false );
       }
       else if( x < elPos )
       {
         x = elPos;

         ecellX = ( x & 0xF ) << 1; ecellY = ( x & 0xFFFFFFFFFFFFFFF0L ) >> 4;

         return( false );
       }
     }

     //Exit edit mode if byte is undefined.

     x -= offset; if( x < data.length && x > 0 && udata[(int)x] ) { endEdit(); return( false ); }

     return( true );
   }

   //End edit mode safely.

   public void endEdit() { if( emode ) { emode = false; checkEdit(); setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); repaint(); } }

   //Editor controls.

   public void keyPressed( KeyEvent e )
   {
     if( emode )
     {
       int c = e.getKeyCode();

       if (c == e.VK_UP)
       {
         checkEdit(); ecellY -= 1; if( !Virtual && ecellY < 0 ) { ecellY = 0; } canEdit();
         
         try { if( !Virtual ) { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } else { IOStream.seekV( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } } catch( Exception er ) { }
       }
       else if (c == e.VK_DOWN)
       {
         checkEdit(); ecellY += 1; canEdit();
         
         try { if( !Virtual ) { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } else { IOStream.seekV( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } } catch( Exception er ) { }
       }

       else if (c == e.VK_LEFT)
       {
         if( ecellX % 2 == 0 ) { checkEdit(); }

         if( !etext ) { ecellX -= 1; } else { ecellX -= 2; } if( ecellX < 0 ){ ecellX = 31; ecellY -= 1; }

         if( !Virtual && ecellY < 0 ) { ecellY = 0; }

         try
         {
           if( etext || ecellX % 2 == 1 ) { canEdit(); IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); }

           else { repaint(); }

         } catch( Exception er ) { }
       }
       else if (c == e.VK_RIGHT)
       {
         if( ecellX % 2 == 1 ) { checkEdit(); }

         if( !etext ) { ecellX += 1; } else { ecellX += 2; } if( ecellX > 31 ){ ecellX = 0; ecellY += 1; }

         if( !Virtual && ecellY < 0 ) { ecellY = 0; }

         try
         {
           if( etext || ecellX % 2 == 0 ) { canEdit(); if( !Virtual ) { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } else { IOStream.seekV( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } }

           else { repaint(); }

         } catch( Exception er ) { }
       }
       else if( c == e.VK_ENTER || c == e.VK_ESCAPE ) { endEdit(); }
       else
       {
         //If events are not disabled we can possibly trigger an event and have out data wrote to the wrong location.

         IOStream.Events = false;

         //Validate text input.

         if( etext && c >= 0x20 )
         {
           try { canEdit(); IOStream.seek( ( ( ecellX >> 1 ) + ( ecellY << 4 ) ) ); } catch( Exception er ) { }

           x = (int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset );

           data[x] = (byte)e.getKeyChar();
             
           try { IOStream.Events = true; IOStream.write( data[x] ); } catch( Exception er ) { }

           ecellX += 2; if( ecellX > 31 ){ ecellX = 0; ecellY += 1; }

           canEdit();
         }

         //Validate hex input.

         else if (c >= 0x41 && c <= 0x46 || c >= 0x30 && c <= 0x39)
         {
           c = c <= 0x39 ? c & 0x0F : c - 0x37;

           try { canEdit(); if( !Virtual ) { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } else { IOStream.seekV( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } } catch( Exception er ) { }

           x = (int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset );

           data[x] = (byte)( ecellX & 1 ) > 0 ? (byte)( ( data[x] & 0xF0 ) | c ) : (byte) ( ( data[x] & 0x0F ) | c << 4 );

           ecellX += 1; if( ecellX > 31 ){ ecellX = 0; ecellY += 1; }

           try { if( ecellX % 2 == 0 || elSelection ) { IOStream.Events = true; IOStream.write( data[x] ); wr = false; } else { wr = true; } } catch( Exception er ) { }

           canEdit(); repaint();
         }
       }
     }
   }

   public void keyTyped( KeyEvent e ) { }
  
  //On seeking a new position in stream.
  
  public void onSeek( IOEvent e )
  {
    SelectC = new Color( 57, 105, 138, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = sel; } else if( IOStream.isMaped() ) { sel = e.SPosV(); sele = sel; }

    if( ( sel - offset ) >= Rows * 16 || ( sel - offset ) < 0 ) { ScrollBar.setValue( sel & 0xFFFFFFFFFFFFFFF0L ); updateData(); }
    
    if( !Virtual || IOStream.isMaped() ) { repaint(); }
  }
  
  //On Reading bytes in stream.
  
  public void onRead( IOEvent e )
  {
    endEdit(); SelectC = new Color( 33, 255, 33, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = e.EPos(); } else if( IOStream.isMaped() ) { sel = e.SPosV(); sele = e.EPosV(); }

    if( ( sel - offset ) >= Rows << 4 || ( sel - offset ) < 0 ) { ScrollBar.setValue( sel & 0xFFFFFFFFFFFFFFF0L ); updateData(); } 
    
    if( !Virtual || IOStream.isMaped() ) { repaint(); }
  }
  
  //On writing bytes in stream.
  
  public void onWrite( IOEvent e )
  {
    SelectC = new Color( 255, 33, 33, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = e.EPos(); } else if( IOStream.isMaped() ) { sel = e.SPosV(); sele = e.EPosV(); }

    if( ( sel - offset ) > Rows << 4 ) { ScrollBar.setValue( sel & 0xFFFFFFFFFFFFFFF0L ); }

    if( !Virtual || IOStream.isMaped() ) { updateData(); repaint(); }
  }

  //Text cursor line animation.

  public void run() 
  {
    try { while( emode ) { Thread.sleep(500); drawc = !drawc; repaint(); } } catch( Exception er ) { }

    //Slide selection animation.

    try
    {
      while( slide != 0 )
      {
        Thread.sleep(20);
        
        if( slide > 0 ) { ScrollBar.setValue( offset + 16 ); sele += 16; } else { if( offset > 0 || Virtual ) { ScrollBar.setValue( offset - 16 ); sele -= 16; } }

        updateData(); repaint();
      }
    } catch( Exception er ) { }
  }
}
