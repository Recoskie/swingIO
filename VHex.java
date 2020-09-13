import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class VHex extends JComponent implements IOEventListener, MouseWheelListener, MouseMotionListener, MouseListener
{
  //The file system stream reference that will be used.

  private RandomAccessFileV IOStream;

  //Monitor when scrolling.
  
  private boolean isScrolling = false;

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

  //Pixel width and height of default font.

  private int pwidth = 0, pheight = 0;

  //Hex editor offset.

  private long offset = 0;

  //Start and end index of selected bytes.

  private long sel = 0, sele = 0;

  //The address column.

  private String s = "Offset (h)";

  //Update data.

  public void updateData()
  {
    //Read data at scroll position.

    int rd = 0, pos = 0, end = 0;
      
    udata = new boolean[data.length];

    try
    {
      //If offset mode use offset seek.

      if (!Virtual)
      {
        IOStream.Events = false;

        //backup current address. 
          
        long t = IOStream.getFilePointer();
          
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

        long t = IOStream.getVirtualPointer();

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
    catch (java.io.IOException e1) {}

    repaint();
  }
  
  //Modified scrollbar class to Handel the full 64 bit address space.
  //Virtual space is treated as unsigned. Except physical file positions.
  
  private class LongScrollBar extends JScrollBar
  {
    private long End = 0, VisibleEnd = 0;
    private int RelUp = 0x70000000, RelDown = 0x10000000;
    private int ov = 0;
    
    public LongScrollBar(int orientation, int value, int visible, int minimum, long maximum)
    {
      super( orientation, value, visible, minimum, Long.compareUnsigned( maximum, 0x7FFFFFF0 ) > 0 ? 0x7FFFFFF0 : (int) ( ( maximum + 15 ) & 0x7FFFFFF0 ) );
      End = maximum; VisibleEnd = End - visible;
    }
    
    @Override public void setValue( int v )
    {
      if( Long.compareUnsigned( VisibleEnd, 0x7FFFFFF0 ) > 0 )
      {
        offset += ( v - ov );

        if( ov < v && v > RelUp ) {  v = RelUp; }
        
        else if( ov > v && v < RelDown ) { v = RelDown; }
      }
      else { offset = v; }

      ov = v;
      
      super.setValue( v & 0x7FFFFFF0 );
      
      isScrolling = false;

      updateData();
    }
    
    @Override public void setVisibleAmount( int v )
    {
      super.setVisibleAmount( v ); VisibleEnd = End - v; setValue( offset + super.getValue() );
    }
    
    public void setValue( long v )
    {
      isScrolling = true;
      
      offset = v; v &= 0x7FFFFFF0;
      
      if( v > RelUp ) { v = RelUp; }
      
      if( v < VisibleEnd ){ v = VisibleEnd; }

      ov = (int)v;
      
      super.setValue( ( (int) v ) & 0x7FFFFFF0 );
      
      isScrolling = false;
    }
  }
  
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

    try { ScrollBar = new LongScrollBar(JScrollBar.VERTICAL, 16, 0, 0, Virtual ? 0xFFFFFFFFFFFFFFFFL : IOStream.length() ); } catch (java.io.IOException e) {}
    
    ScrollBar.setUnitIncrement( 16 );

    //Custom selection handling.

    super.addMouseListener(this); super.addMouseMotionListener(this);

    //Add scroll wheal handler.
    
    super.addMouseWheelListener(this);

    //Add everything to main component.

    super.setLayout(new BorderLayout()); super.add(ScrollBar, BorderLayout.EAST);
  }

  //Basic graphics.

  private int index = 0, cell = 0, addcol = 0, endw = 0;
  private int cpos = 0;

  public void paintComponent(Graphics g)
  {
    //Initialize once.

    if( pwidth == 0 )
    {
      java.awt.FontMetrics fm = g.getFontMetrics(g.getFont());

      pwidth = fm.stringWidth("C"); pheight = fm.getHeight();

      cell = pwidth * 2 + 1;
      
      addcol = pwidth * 18 + 2;

      endw = cell * 16 + addcol;

      cpos = ( addcol / 2 ) - ( fm.stringWidth(s) / 2 );
    }

    if( Rows != ( getHeight() / pheight ) ) { Rows = getHeight() / pheight; data = java.util.Arrays.copyOf( data, Rows * 16 ); updateData(); return; }

    //Begin Graphics for hex editor component.

    g.setColor(new Color(238,238,238)); g.fillRect(0,0,pheight,endw);

    g.setColor(Color.white);

    g.fillRect(addcol,pheight,endw-addcol,getHeight());

    Selection( g );

    g.setColor(Color.black);

    g.drawString( s, cpos, pheight - 3 );

    g.fillRect(0,pheight,addcol,getHeight());

    for(int i1 = addcol, index = 0; i1 < endw; i1 += cell, index++ )
    {
      g.drawString( String.format( "%1$02X", index ), i1 + 1 , pheight - 3 ); g.drawLine( i1, pheight, i1, getHeight() );
    }

    g.drawLine( endw, pheight, endw, getHeight() );
		
    for(int i1 = pheight, index = 0; i1 < getHeight(); i1 += pheight )
    {
      g.drawLine( addcol, i1, endw, i1 );

      for(int i2 = addcol; i2 < endw; i2 += cell, index++ )
      {
        g.drawString( udata[index] ? "??" : String.format( "%1$02X", data[index] ), i2 + 1, i1 + pheight - 3 );
      }
    }

    g.setColor(Color.white);

    for(int i1 = pheight, index = 0; i1 < getHeight(); i1 += pheight, index += 16 )
    {
      g.drawString( "0x" + String.format( "%1$016X", offset + index), 3, i1 + pheight - 3 );
    }
  }

  //Selection Handler.

  private int y = 0, y2 = 0;
  private long t = 0;

  private void Selection(Graphics g)
  {
    g.setColor(SelectC);

    if( sel > sele) { t = sele; sele = sel; sel = t; }
    
    y = (int)(sel - offset) >> 4; y2 = (int)(sele - offset) >> 4; y2 -= y; y2 += 1; y += 1;

    if( y < 1 ){ y2 += y - 1; y = 1; }

    if( y > 0 ) { g.fillRect( addcol, y * pheight, endw - addcol, y2 * pheight ); }

    //Clear the start and end pos.

    g.setColor(Color.white);

    if( ( sel & 0xF ) >= 0x1 ) { g.fillRect( addcol, y * pheight, (int)(sel & 0xF) * cell, pheight ); }

    y += y2 - 1; y2 = addcol + ( ( (int)sele + 1 ) & 0xF )  * cell;
    
    if( (sele & 0xF) <= 0xE ) { g.fillRect( y2, y * pheight, endw - y2, pheight ); }

    if( t != 0 ) { t = sele; sele = sel; sel = t; t = 0; }
  }
  
  //Adjust scroll bar on scroll wheal.
  
  @Override public void mouseWheelMoved(MouseWheelEvent e)
  { 
    ScrollBar.setValue( Math.max( 0, offset + ( e.getUnitsToScroll() << 4 ) ) );
  }

  public void mouseMoved(MouseEvent e) { }

  public void mouseDragged(MouseEvent e)
  {
    int x = e.getX(); if( x > endw ) { x = endw - 1; }

    x -= addcol; if ( x < 0 ) { x = 0; }

    x = ( x / cell ) & 0xF;
    
    int y = ( e.getY() / pheight ) - 1; y <<= 4;

    sele = x + y + offset; repaint();
  }

  public void mouseExited(MouseEvent e) { }

  public void mouseEntered(MouseEvent e) { }

  public void mouseReleased(MouseEvent e) { }

  public void mousePressed(MouseEvent e)
  {
    int x = e.getX(); if( x > endw ) { x = endw - 1; }

    x -= addcol; if ( x < 0 ) { x = 0; }

    x = ( x / cell ) & 0xF;
    
    int y = ( e.getY() / pheight ) - 1; y <<= 4;

    sel = x + y + offset;

    try{ if( !Virtual ) { IOStream.seek( x + y + offset ); } else { IOStream.seekV( x + y + offset ); } } catch( Exception er ) { }
  }

  //Begin hex edit mode.

  public void mouseClicked(MouseEvent e)
  {
    
  }
  
  //On seeking a new position in stream.
  
  public void onSeek( IOEvent e )
  {
    SelectC = new Color( 57, 105, 138, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = sel; } else { sel = e.SPosV(); sele = sel; }

    if( ( offset - sel ) > Rows * 16 ) { offset = sel; }

    repaint();
  }
  
  //On Reading bytes in stream.
  
  public void onRead( IOEvent e )
  {
    SelectC = new Color( 33, 255, 33, 128 );
  }
  
  //On writing bytes in stream.
  
  public void onWrite( IOEvent e )
  {
    SelectC = new Color( 255, 33, 33, 128 );
  }
}
