import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

  //Pixel width and height of default font.

  private int pwidth = 0, pheight = 0;

  //Hex editor offset.

  private long offset = 0;

  //Start and end index of selected bytes.

  private long sel = 0, sele = 0;

  //The address column.

  private String s = "Offset (h)";

  //Edit mode.

  private boolean emode = false;
  private long ecellX = 0, ecellY = 0;
  private boolean drawc = false;

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

      updateData();
    }
    
    @Override public void setVisibleAmount( int v )
    {
      super.setVisibleAmount( v ); VisibleEnd = End - v; setValue( offset + super.getValue() );
    }
    
    public void setValue( long v )
    {
      offset = v; v &= 0x7FFFFFF0;
      
      if( v > RelUp ) { v = RelUp; }
      
      if( v < VisibleEnd ){ v = VisibleEnd; }

      ov = (int)v;
      
      super.setValue( ( (int) v ) & 0x7FFFFFF0 );
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

    //Key listener for edit mode.

    super.addKeyListener(this);

    //Add scroll bar to component.

    super.setLayout(new BorderLayout()); super.add(ScrollBar, BorderLayout.EAST);
  }

  public void addNotify() { super.addNotify(); requestFocus(); }

  //Basic graphics.

  private int index = 0, cell = 0, addcol = 0, endw = 0;
  private int cpos = 0;
  private int x = 0, y = 0;
  private long t = 0;

  public void paintComponent( Graphics g )
  {
    //Initialize once.

    if( pwidth == 0 )
    {
      java.awt.FontMetrics fm = g.getFontMetrics(g.getFont());

      pwidth = fm.stringWidth("C"); pheight = fm.getHeight();

      cell = ( pwidth << 1 ) + 1;
      
      addcol = pwidth * 18 + 2;

      endw = cell * 16 + addcol;

      cpos = ( addcol >> 1 ) - ( fm.stringWidth(s) >> 1 );
    }

    if( Rows != ( getHeight() / pheight ) ) { Rows = getHeight() / pheight; data = java.util.Arrays.copyOf( data, Rows << 4 ); updateData(); return; }

    //Begin Graphics for hex editor component.

    g.setColor(new Color(238,238,238)); g.fillRect(0,0,pheight,endw);

    g.setColor(Color.white);

    g.fillRect(addcol,pheight,endw-addcol,getHeight());

    //Select character in edit mode.

    Selection( g );

    if( emode )
    {
      g.setColor(new Color( 57, 105, 138, 128 ));

      //Cell alignment.

      x = ( ( ( (int)ecellX >> 1 ) ) * cell ) + addcol;

      //Character alignment.
      
      x += ( ( (int)ecellX ) & 1 ) * ( cell >> 1 );

      y = ( (int)ecellY - (int)( offset >> 4 ) + 1 ) * pheight;

      g.fillRect( x, y, cell >> 1, pheight );
    }

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

    //Draw Cursor line.

    if( emode && drawc )
    {
      //Cell alignment.

      x = ( ( ( (int)ecellX >> 1 ) + 1 ) * cell ) + addcol;

      //Character alignment.
      
      x -= ( ( (int)ecellX + 1 ) & 1 ) * ( cell >> 1 );

      //Border.
      
      x -= 2;

      y = ( (int)ecellY - (int)( offset >> 4 ) + 1 ) * pheight;

      g.drawLine( x, y, x, y + pheight );
    }

    //Address offset.

    g.setColor(Color.white);

    for(int i1 = pheight, index = 0; i1 < getHeight(); i1 += pheight, index += 16 )
    {
      g.drawString( "0x" + String.format( "%1$016X", offset + index), 3, i1 + pheight - 3 );
    }
  }

  //Selection Handler.

  private void Selection( Graphics g )
  {
    g.setColor(SelectC);

    if( sel > sele) { t = sele; sele = sel; sel = t; }
    
    x = (int)(sel - offset) >> 4; y = (int)(sele - offset) >> 4; y -= x; y += 1; x += 1;

    if( x < 1 ){ y += x - 1; x = 1; }

    if( x > 0 ) { g.fillRect( addcol, x * pheight, endw - addcol, y * pheight ); }

    //Clear the start and end pos.

    g.setColor(Color.white);

    if( sel - offset >= 0 ) { g.fillRect( addcol, x * pheight, (int)(sel & 0xF) * cell, pheight ); }

    x += y - 1; y = addcol + ( ( (int)sele + 1 ) & 0xF )  * cell;
    
    if( y > addcol ) { g.fillRect( y, x * pheight, endw - y, pheight ); }

    if( emode )
    {
      g.fillRect( (int)( addcol + ( ecellX >> 1 ) * cell ), (int)( ( ecellY - ( offset >> 4 ) + 1 ) * pheight ), cell, pheight );
    }

    if( t != 0 ) { t = sele; sele = sel; sel = t; t = 0; }
  }
  
  //Adjust scroll bar on scroll wheal.
  
  public void mouseWheelMoved( MouseWheelEvent e )
  {
    offset += e.getUnitsToScroll() << 4; if( !Virtual && offset < 0 ) { offset = 0; } updateData();
  }

  public void mouseMoved( MouseEvent e )
  {
    if(emode)
    {
      if( e.getX() > addcol && e.getX() < endw && e.getY() > pheight )
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
    if( !emode )
    {
      x = e.getX(); if( x > endw ) { x = endw - 1; }

      x -= addcol; if ( x < 0 ) { x = 0; }

      x = ( x / cell ) & 0xF;
    
      y = ( e.getY() / pheight ) - 1; y <<= 4;

      sele = x + y + offset; repaint();
    }
  }

  public void mouseExited( MouseEvent e ) { }

  public void mouseEntered( MouseEvent e ) { }

  public void mouseReleased( MouseEvent e ) { }

  public void mousePressed( MouseEvent e )
  {
    x = e.getX(); if( x > endw ) { x = endw - 1; }

    x -= addcol; if ( x < 0 ) { x = 0; }

    x = ( x / cell ) & 0xF;
    
    y = ( e.getY() / pheight ) - 1; y <<= 4;

    sel = x + y + offset;

    if (emode && e.getX() > addcol && e.getX() < endw && e.getY() > pheight)
    {
      checkEdit();

      ecellX = ( e.getX() - addcol ) / cell;

      ecellX = (  ecellX << 1 ) + ( ( ( e.getX() - addcol ) % cell ) / ( cell >> 1 ) );

      ecellY = ( e.getY() / pheight ) + (int)( offset >> 4 ); ecellY -= 1; canEdit();
    }

    try{ if( !Virtual ) { IOStream.seek( x + y + offset ); } else { IOStream.seekV( x + y + offset ); } } catch( Exception er ) { }
  }

  //Begin hex edit mode.

  public void mouseClicked( MouseEvent e )
  {
    if( e.getClickCount() == 2 && e.getX() > addcol && e.getX() < endw && e.getY() > pheight )
    {
      ecellX = ( e.getX() - addcol ) / cell;

      ecellX = (  ecellX << 1 ) + ( ( ( e.getX() - addcol ) % cell ) / ( cell >> 1 ) );

      ecellY = ( e.getY() / pheight ) + (int)( offset >> 4 ); ecellY -= 1;

      if( !udata[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] )
      {
        setCursor(new Cursor(Cursor.TEXT_CURSOR));

        try{ if( !emode ) { new Thread(this).start(); } } catch ( Exception er ) {}

        emode = true; repaint();
      }
    }
  }

   public void keyReleased( KeyEvent e ) { }

   private boolean wr = false;

   //Writes modified byte before moving.

   private void checkEdit()
   {
     if( wr )
     {
       try { IOStream.write( data[(int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset )] ); wr = false; } catch( Exception er ) {}
     }
   }

   //Exit edit mode if byte is undefined.

   private void canEdit() { x = (int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset ); if( x < data.length && x > 0 && udata[x] ) { endEdit(); } }

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
         checkEdit(); ecellY -= 1; canEdit(); try { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } catch( Exception er ) { }
       }
       else if (c == e.VK_DOWN)
       {
         checkEdit(); ecellY += 1; canEdit(); try { IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); } catch( Exception er ) { }
       }

       else if (c == e.VK_LEFT)
       {
         if( ecellX % 2 == 0 ) { checkEdit(); }

         ecellX -= 1; if( ecellX < 0 ){ ecellX = 31; ecellY -= 1; }

         try
         {
           if( ecellX % 2 == 1 ) { canEdit(); IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); }

           else { repaint(); }

         } catch( Exception er ) { }
       }
       else if (c == e.VK_RIGHT)
       {
         if( ecellX % 2 == 1 ) { checkEdit(); }

         ecellX += 1; if( ecellX > 31 ){ ecellX = 0; ecellY += 1; }

         try
         {
           if( ecellX % 2 == 0 ) { canEdit(); IOStream.seek( ( ecellX >> 1 ) + ( ecellY << 4 ) ); }

           else { repaint(); }

         } catch( Exception er ) { }
       }
       else if( c == e.VK_ENTER || c == e.VK_ESCAPE ) { endEdit(); }
       else
       {
         //Validate hex input.

         if (c >= 0x41 && c <= 0x46 || c >= 0x30 && c <= 0x39)
         {
           c = c <= 0x39 ? c & 0x0F : c - 0x37;

           x = (int)( ( ecellX >> 1 ) + ( ecellY << 4 ) - offset );

           data[x] = (byte)( ecellX & 1 ) > 0 ? (byte)( ( data[x] & 0xF0 ) | c ) : (byte) ( ( data[x] & 0x0F ) | c << 4 );

           ecellX += 1; if( ecellX > 31 ){ ecellX = 0; ecellY += 1; }

           try { if( ecellX % 2 == 0 ) { IOStream.write( data[x] ); wr = false; } else { wr = true; } } catch( Exception er ) { }

           repaint();
         }
       }
     }
   }

   public void keyTyped( KeyEvent e ) { }
  
  //On seeking a new position in stream.
  
  public void onSeek( IOEvent e )
  {
    SelectC = new Color( 57, 105, 138, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = sel; } else { sel = e.SPosV(); sele = sel; }

    if( ( sel - offset ) >= Rows * 16 || ( sel - offset ) < 0 ) { offset = sel & 0xFFFFFFFFFFFFFFF0L; updateData(); } else { repaint(); }
  }
  
  //On Reading bytes in stream.
  
  public void onRead( IOEvent e )
  {
    endEdit(); SelectC = new Color( 33, 255, 33, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = e.EPos(); } else { sel = e.SPosV(); sele = e.EPosV(); }

    if( ( sel - offset ) >= Rows << 4 || ( sel - offset ) < 0 ) { offset = sel & 0xFFFFFFFFFFFFFFF0L; updateData(); } else { repaint(); }
  }
  
  //On writing bytes in stream.
  
  public void onWrite( IOEvent e )
  {
    SelectC = new Color( 255, 33, 33, 128 );

    if( !Virtual ) { sel = e.SPos(); sele = e.EPos(); } else { sel = e.SPosV(); sele = e.EPosV(); }

    if( ( sel - offset ) > Rows << 4 ) { offset = sel & 0xFFFFFFFFFFFFFFF0L; }

    updateData();
  }

  //Text cursor line animation.

  public void run() { try { while( emode ) { Thread.sleep(500); drawc = !drawc; repaint(); } } catch( Exception er ) {} }
}
