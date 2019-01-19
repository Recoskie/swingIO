import java.io.*;
import java.util.*;
import javax.swing.event.*;

//Event constructor.

<<<<<<< HEAD
class IOEvent extends EventObject
{
  private long TPos = 0;
  private long End = 0;
  
  public IOEvent( Object source ) { super( source ); }
  
  public IOEvent( Object source, long TPos, long End )
  {
    super( source ); this.TPos = TPos; this.End = End;
  }
  
  public long SPos(){ return( TPos ); }
  
  public long EPos(){ return( End ); }
  
  public long length(){ return( End - TPos ); }
}
=======
  RandomAccessFileV IOStream;

  //The end of the data stream.

  long End = 0;

  //The table which will update as you scroll through the IO stream.

  JTable tdata;

  //Number of rows in draw space.

  int TRows = 0;

  //The table model.
>>>>>>> parent of a26eaea... Update VHex.java

//Basic IO Events.

interface IOEventListener extends EventListener
{
  public void onSeek( IOEvent evt );
  public void onRead( IOEvent evt );
  public void onWrite( IOEvent evt );
}

public class RandomAccessFileV extends RandomAccessFile implements Runnable
{
  //My event listener list for graphical components that listen for stream update events.
  
  protected EventListenerList list = new EventListenerList();
  
<<<<<<< HEAD
  //Disable events. This is to stop graphics components from updating while doing intensive operations.
  
  public boolean Events = true;
  
  //Trigger Position.
  
  private long TPos = 0;
  
  //Updated pos.
  
  private long pos = 0;
  
  //Running thread.
  
  private boolean Running = false;
  
  //Event trigger.
  
  private boolean Trigger = false;
  
  //Read or write event.
  
  private boolean Read = false;
  
  //Main event thread.
  
  private Thread EventThread;

  //Add and remove event listeners.

  public void addMyEventListener( IOEventListener listener )
  {
    //Event thread is created for sequential read, or write length.
    
    if( !Running ) { EventThread = new Thread(this); EventThread.start(); }
    
    list.add( IOEventListener.class, listener ); Events = true;
  }
  
  public void removeMyEventListener( IOEventListener listener )
  {
    list.remove( IOEventListener.class, listener );
    
    //If all event listeners are removed. Disable event thread.
    
    Running = ( list.getListenerList().length > 0 ); Events = false;
  }
  
  //Fire the event to all my graphics components, for editing the stream, or decoding data types.
  
  void fireIOEventSeek ( IOEvent evt )
  {
    Object[] listeners = list.getListenerList();
    
    if ( Events )
    {
      for ( int i = 0; i < listeners.length; i = i + 2 )
      {
        if ( listeners[i] == IOEventListener.class )
        {
          ((IOEventListener)listeners[i+1]).onSeek( evt );
        }
      }
    }
  }

  //This is a delayed event to find the length of the data, for sequential read or write.
  
  void fireIOEvent ( IOEvent evt )
=======
  //The hex editors scroll bar.

  JScrollBar ScrollBar;

  //Enable relative scrolling for files larger than 4Gb.

  boolean Rel = false;

  //Position that is relative to scroll bar position.

  long RelPos = 0;

  //Virtual mode, or offset mode.

  private boolean Virtual = false;

  //The main hex edior display.

  class AddressModel extends AbstractTableModel
>>>>>>> parent of a26eaea... Update VHex.java
  {
    Object[] listeners = list.getListenerList();
    
    if ( Events )
    {
      for ( int i = 0; i < listeners.length; i = i + 2 )
      {
        if ( listeners[i] == IOEventListener.class )
        {
          if( Read )
          {
            ((IOEventListener)listeners[i+1]).onRead( evt );
          }
          
          else
          {
            ((IOEventListener)listeners[i+1]).onWrite( evt );
          }
        }
      }
    }
  }
  
  //64 bit address pointer. Used by things in virtual ram address space such as program instructions, and data.
  
  private long VAddress = 0x0000000000000000L;
  
  //Positions of an file can be mapped into ram address space locations.

  private class VRA
  {
    //General address map properties.
    
    private long Pos = 0x0000000000000000L, Len = 0x0000000000000000L, VPos = 0x0000000000000000L, VLen = 0x0000000000000000L;
    
    //File offset end position.
    
    private long FEnd = 0x0000000000000000L;
    
    //Virtual address end position. If grater than actual data the rest is 0 filled space.
    
    private long VEnd = 0x0000000000000000L;
    
    //Construct area map. Both long/int size. Note End position can match the start position as the same byte. End position is minus 1.
    
    public VRA( long Offset, long DataLen, long Address, long AddressLen )
    {
      Pos = Offset; Len = DataLen; VPos = Address; VLen = AddressLen;
      
      //Negative not allowed because of java's signified compare.
      
      if( Pos < 0 ) { Pos = 0; } if( VPos < 0 ) { VPos = 0; }
      if( Len < 0 ) { Len = 0; } if( VLen < 0 ) { VLen = 0; }
      
      //Data offset length can't be higher than virtual offset length.
      
      if( Len > VLen ){ Len = VLen; }
      
      //Calculate file offset end positions and virtual end positions.
      
      FEnd = Pos + ( Len - 1 ); VEnd = VPos + ( VLen - 1 );
    }
    
    //Set the end of an address when another address writes into this address.
    
    public void setEnd( long Address )
    {
      //Set end of the current address to the start of added address.
      
<<<<<<< HEAD
      VEnd = Address;
=======
      long pos = row * RowLen;
      
      try { pos += Virtual ? IOStream.getVirtualPointer() : IOStream.getFilePointer(); } catch ( java.io.IOException e ) {}
>>>>>>> parent of a26eaea... Update VHex.java
      
      //Calculate address length.
      
<<<<<<< HEAD
      VLen = ( VEnd + 1 ) - VPos;
      
      //If there still is data after the added address.
      
      Len = Math.min( Len, VLen ); 
      
      //Calculate the bytes written into.
      
      FEnd = Pos + ( Len - 1 );
=======
      if (col == 0)
      {
        return ("0x" + String.format("%1$016X", pos & 0xFFFFFFFFFFFFFFF0L));
      }

      //Else byte to hex.

      else if ( ( pos + (col - 1) ) < End )
      {
        return (String.format("%1$02X", data[ (row * RowLen) + (col - 1) ]));
      }

      return ("??");
>>>>>>> parent of a26eaea... Update VHex.java
    }
    
    //Addresses that write over the start of an address.
    
    public void setStart( long Address )
    {
      //Add Data offset to bytes written over at start of address.
        
      Pos += Address - VPos;
        
      //Move Virtual address start to end of address.
        
      VPos = Address;
        
      //Recalculate length between the new end position.
        
      Len = ( FEnd + 1 ) - Pos; VLen = ( VEnd + 1 ) - VPos;
    }
    
    //String Representation for address space.
    
    public String toString()
    {
      return( "File(Offset)=" + String.format( "%1$016X", Pos ) + "---FileEnd(Offset)=" + String.format( "%1$016X", FEnd ) + "---Start(Address)=" + String.format( "%1$016X", VPos ) + "---End(Address)=" + String.format( "%1$016X", VEnd ) + "---Length=" + VLen );
    }
  }
  
  //The mapped addresses.
  
  private java.util.ArrayList<VRA> Map = new java.util.ArrayList<VRA>();
  
  //The virtual address that the current virtual address pointer is in range of.
  
  private VRA curVra;
  
  //Speeds up search. By going up or down from current virtual address.
  
  private int Index = -1;
  
  //Map.size() is slower than storing the mapped address space size.
  
  private int MSize = 1;
  
  //Construct the reader using an file, or disk drive.
  
  public RandomAccessFileV( File file, String mode ) throws FileNotFoundException { super( file, mode ); Map.add( new VRA( 0, 0, 0, 0x7FFFFFFFFFFFFFFFL ) ); curVra = Map.get(0); }
  
  public RandomAccessFileV( String name, String mode ) throws FileNotFoundException { super( name, mode ); Map.add( new VRA( 0, 0, 0, 0x7FFFFFFFFFFFFFFFL ) ); curVra = Map.get(0); }
  
  //Temporary read only data.
  
  private static File TFile;
  
  private static File mkf() throws IOException { TFile = File.createTempFile("",".tmp"); TFile.deleteOnExit(); return( TFile ); }
  
  public RandomAccessFileV( byte[] data ) throws IOException
  {
    super( mkf(), "r" ); super.write( data );
    
    Map.add( new VRA( 0, data.length, 0, data.length ) ); curVra = Map.get(0);
    
    TFile.delete();
  }
  
  public RandomAccessFileV( byte[] data, long Address ) throws IOException
  {
    super( mkf(), "r" ); super.write( data );
    
    Map.add( new VRA( 0, (long)data.length, Address, (long)data.length ) ); curVra = Map.get(0);
    
    TFile.delete();
  }

  //Reset the Virtual ram map.
  
  public void resetV()
  {
    Map.clear();
    
    Map.add( new VRA( 0, 0, 0, 0x7FFFFFFFFFFFFFFFL ) );
    
    MSize = 1; Index = -1; VAddress = 0;
  }
  
  //Get the virtual address pointer. Relative to the File offset pointer.
  
  public long getVirtualPointer() throws IOException { return( super.getFilePointer() + VAddress ); }
  
  //Add an virtual address.
  
  public void addV( long Offset, long DataLen, long Address, long AddressLen ) 
  {
    VRA Add = new VRA( Offset, DataLen, Address, AddressLen );
    VRA Cmp = null;
    
    //The numerical range the address lines up to in index in the address map.
    
    int e = 0;
    
    //fixes lap over ERROR.
    
    boolean sw = true;
    
    //If grater than last address then add to end.
    
    if( MSize > 0 && Add.VPos > Map.get( MSize - 1 ).VEnd ){ Map.add( Add ); MSize++; return; }
    
    //Else add and write in alignment.
    
    for( int i = 0; i < MSize; i++ )
    {
      Cmp = Map.get( i );
      
      //If the added address writes to the end, or in the Middle of an address.
      
      if( Add.VPos <= Cmp.VEnd && Add.VPos > Cmp.VPos && sw )
      {
<<<<<<< HEAD
        //Address range position.
        
        e = i + 1;
        
        //If the added address does not write to the end of the address add it to the next element.
        
        if( Cmp.VEnd > Add.VEnd )
        {
          sw = false; Map.add( e, new VRA( Cmp.Pos, Cmp.Len, Cmp.VPos, Cmp.VLen ) ); MSize++;
=======
        //If offset mode use offset seek, and write.

        if (!Virtual)
        {
          IOStream.write(b);
          IOStream.seek((row * RowLen) + (col - 1) + IOStream.getFilePointer());
        }

        //If Virtual use Virtual map seek, and write.

        else
        {
          IOStream.writeV(b);
          IOStream.seekV((row * RowLen) + (col - 1) + IOStream.getVirtualPointer());
>>>>>>> parent of a26eaea... Update VHex.java
        }
        
        //Set end of the current address to the start of added address.
        
        Cmp.setEnd( Add.VPos - 1 );
      }
      
      //If added Address writes to the start of Address.
      
      else if( Add.VPos <= Cmp.VPos && Cmp.VPos <= Add.VEnd || !sw )
      {        
        //Address range position.
        
        e = i;
        
        //Add Data offset to bytes written over at start of address.
        
        Cmp.setStart( Add.VEnd + 1 );
        
        //Remove overwritten addresses that are negative in length remaining.
        
        if( Cmp.VLen <= 0 ) { Map.remove( i ); i--; MSize--; }
        
        //Else if 0 or less data. Set data length and offset 0.
        
        else if( Cmp.Len <= 0 ){ Cmp.Pos = 0; Cmp.Len = 0; Cmp.FEnd = 0; }
        
        sw = true;
      }
    }
    
    //Add address in order to it's position in range.
    
    Map.add( e, Add ); MSize++;
    
    //If added address lines up with Virtual address pointer. Seek the new address position.
    
    try { if( VAddress >= Add.VPos && VAddress <= Add.VEnd ) { seekV( VAddress ); } } catch( IOException ex ) {  }
  }
  
  //Adjust the Virtual offset pointer relative to the mapped virtual ram address and file pointer.
  
  public void seekV( long Address ) throws IOException
  {
    //If address is in range of current address index.
    
    if( Address >= curVra.VPos && Address <= ( curVra.VPos + curVra.Len ) )
    {
      super.seek( ( Address - curVra.VPos ) + curVra.Pos );
      
      VAddress = Address - super.getFilePointer();
    }
    
    //If address is grater than the next vra iterate up in indexes.
    
    else if( Address >= curVra.VPos || Index == -1 )
    {
      VRA e = null;
      
      for( int n = Index + 1; n < MSize; n++ )
      {
        e = Map.get( n );
        
        if( Address >= e.VPos && Address <= ( e.VPos + e.Len ) )
        {
          Index = n; curVra = e;
          
          super.seek( ( Address - e.VPos ) + e.Pos );
          
<<<<<<< HEAD
          VAddress = Address - super.getFilePointer();
=======
          IOStream.seek( t & 0xFFFFFFFFFFFFFFF0L );
          IOStream.read( data );
          IOStream.seek( t );
>>>>>>> parent of a26eaea... Update VHex.java
          
          return;
        }
      }
    }
    
    //else iterate down in indexes.
    
    else if( Address <= curVra.VPos )
    {
      VRA e = null;
      
      for( int n = Index - 1; n > -1; n-- )
      {
        e = Map.get( n );
        
        if( Address >= e.VPos && Address <= ( e.VPos + e.Len ) )
        {
          Index = n; curVra = e;
          
          super.seek( ( Address - e.VPos ) + e.Pos );
          
<<<<<<< HEAD
          VAddress = Address - super.getFilePointer();
=======
          IOStream.seekV( t & 0xFFFFFFFFFFFFFFF0L );
          IOStream.readV( data );
          IOStream.seekV( t );
>>>>>>> parent of a26eaea... Update VHex.java
          
          return;
        }
      }
    }
    
    VAddress = Address - super.getFilePointer(); fireIOEvent( new IOEvent( this, VAddress, VAddress ) );
  }
  
  public int readV() throws IOException
  {
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
    //Read in current offset. If any data to be read.
    
    if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd ) { return( super.read() ); }
    
    //No data then 0 space.
    
    VAddress += 1; return( 0 );
  }
  
  //Read len bytes from current virtual offset pointer.
  
  public int readV( byte[] b ) throws IOException
  {
    int Pos = 0, n = 0;
    
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
    //Start reading.
    
    while( Pos < b.length )
    {
      //Read in current offset.
      
      if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd && curVra.Len > 0 )
      {
        //Number of bytes that can be read from current area.
        
        n = (int)Math.min( ( curVra.FEnd + 1 ) - super.getFilePointer(), b.length );
        
        super.read( b, Pos, n ); Pos += n;
      }
      
      //Else 0 space. Skip n to Next address.
      
      else
      {
        n = (int)( ( curVra.VPos + curVra.Len ) - ( super.getFilePointer() - curVra.Pos ) );
        
        if( n < 0 ) { n = b.length - Pos; }
        
        VAddress += n; Pos += n;
        
        seekV( getVirtualPointer() );
      }
    }
    
    return( 0 );
  }
  
  //Read len bytes at offset to len from current virtual offset pointer.
  
  public int readV( byte[] b, int off, int len ) throws IOException
  {
    int Pos = off, n = 0; len += off;
    
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
    //Start reading.
    
    while( Pos < len )
    {
      //Read in current offset.
      
      if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd && curVra.Len > 0 )
      {
        //Number of bytes that can be read from current area.
        
        n = (int)Math.min( ( curVra.FEnd + 1 ) - super.getFilePointer(), len );
        
        super.read( b, Pos, n ); Pos += n;
      }
      
      //Else 0 space. Skip n to Next address.
      
      else
      {
<<<<<<< HEAD
        n = (int)( curVra.VLen - ( super.getFilePointer() - curVra.Pos ) );
        
        if( n < 0 ) { n = len - Pos; }
        
        VAddress += n; Pos += n;
        
        seekV( getVirtualPointer() );
=======
        //Col += 1;

        if (Col >= 17)
        {
          Col = 1;

          if (Row >= (TRows - 1))
          {
            ScrollBar.setValue(ScrollBar.getValue() + 1);
          }
          else
          {
            Row += 1;
          }
        }

        tdata.editCellAt(Row, Col); tdata.getEditorComponent().requestFocus();
        pos = 0; CellMove = true; return;
>>>>>>> parent of a26eaea... Update VHex.java
      }
    }
    
    return( 0 );
  }
  
  //Write an byte at Virtual address pointer if mapped.
  
  public void writeV( int b ) throws IOException
  {
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
    //Write the byte if in range of address.
    
    if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd ) { super.write( b ); return; }
    
    //Move virtual pointer.
    
    VAddress++;
  }
  
  //Write set of byte at Virtual address pointer to only mapped bytes.
  
  public void writeV( byte[] b ) throws IOException
  {
    int Pos = 0, n = 0;
    
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
<<<<<<< HEAD
    //Start Writing.
    
    while( Pos < b.length )
=======
    super.addComponentListener(new CalcRows());

    Virtual = mode;

    //Reference the file stream.

    IOStream = f;

    TModel = new AddressModel(mode);

    tdata = new JTable(TModel, new AddressColumnModel());

    tdata.createDefaultColumnsFromModel();

    //The length of the stream.

    try
    {
      //If offset mode then end is the end of the stream.

      if (!Virtual)
      {
        End = IOStream.length();

        //Enable relative scrolling if the data length is outside the scroll bar range.

        if (End > 0x7FFFFFFF)
        {
          Rel = true;
        }
      }

      //Else the last 64 bit virtual address. Thus set relative scrolling.

      else { Rel = true; End = 0x7FFFFFFFFFFFFFFFL; }
    }
    catch (java.io.IOException e) {}

    //Columns can not be re-arranged.

    tdata.getTableHeader().setReorderingAllowed(false);

    //Columns can not be re-arranged.

    tdata.getTableHeader().setReorderingAllowed(false);

    //Do not alow resizing of cells.

    tdata.getTableHeader().setResizingAllowed(false);

    //Set the table editor.

    tdata.setDefaultEditor(String.class, new CellHexEditor());

    //Setup Scroll bar system.

    ScrollBar = new JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, End < 0x7FFFFFFF ? (int)((End + 15) / 16) : 0x7FFFFFFF);

    //Custom selection handling.

    tdata.addMouseListener(new MouseAdapter()
    {
      @Override public void mousePressed(MouseEvent e)
      {
        SRow = RelPos + ScrollBar.getValue() + tdata.rowAtPoint(e.getPoint());
        SCol = tdata.columnAtPoint(e.getPoint());

        ERow = SRow; ECol = SCol;

        TModel.fireTableDataChanged();
      }
    });

    tdata.addMouseMotionListener(new MouseMotionAdapter()
>>>>>>> parent of a26eaea... Update VHex.java
    {
      //Write in current offset.
      
      if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd && curVra.Len > 0 )
      {
<<<<<<< HEAD
        //Number of bytes that can be written in current area.
        
        n = (int)Math.min( ( curVra.FEnd + 1 ) - super.getFilePointer(), b.length );
        
        super.write( b, Pos, n ); Pos += n;
      }
      
      //Else 0 space. Skip n to Next address.
      
      else
      {
        n = (int)( curVra.VLen - ( super.getFilePointer() - curVra.Pos ) );
        
        if( n < 0 ) { n = b.length - Pos; }
        
        VAddress += n; Pos += n;
        
        seekV( getVirtualPointer() );
=======
        //Automatically scroll while selecting bytes.

        if (e.getY() > tdata.getHeight())
        {
          ScrollBar.setValue(Math.min(ScrollBar.getValue() + 4, 0x7FFFFFFF));
          ERow = RelPos + ScrollBar.getValue() + (TModel.getRowCount() - 1);
        }
        else if (e.getY() < 0)
        {
          ScrollBar.setValue(Math.max(ScrollBar.getValue() - 4, 0));
          ERow = RelPos + ScrollBar.getValue();
        }
        else
        {
          ERow = RelPos + ScrollBar.getValue() + tdata.rowAtPoint(e.getPoint());
          ECol = tdata.columnAtPoint(e.getPoint());
        }

        //Force the table to rerender cells.

        TModel.fireTableDataChanged();
      }
    });

    //As we scroll update the table data. As it would be insane to graphically render large files in hex.

    class Scroll implements AdjustmentListener
    {
      public void adjustmentValueChanged(AdjustmentEvent e)
      {
        try
        {
          if( !Virtual && IOStream.Events )
          {
            IOStream.seek( RelPos + ScrollBar.getValue() * 16 );
          }
          else if( IOStream.Events )
          {
            IOStream.seekV( RelPos + ScrollBar.getValue() * 16 );
          }
        }
        catch( java.io.IOException e1 ) {}

        //If relative scrolling.

        if (Rel) { }
        
        if (tdata.isEditing()) { tdata.getCellEditor().stopCellEditing(); }
      }
    }

    ScrollBar.addAdjustmentListener(new Scroll());

    //Custom table selection rendering.

    tdata.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
    {
      @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int column)
      {
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, r, column);

        long row = r + RelPos + ScrollBar.getValue();

        //Alternate shades between rows.

        if (row % 2 == 0)
        {
          c.setBackground(Color.white);
          c.setForeground(Color.black);
        }
        else
        {
          c.setBackground(new Color(242, 242, 242));
          c.setForeground(Color.black);
        }

        //If selection is in same row

        if (SRow == ERow && row == SRow)
        {
          if (SCol > ECol && column >= ECol && column <= SCol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
          else if (column <= ECol && column >= SCol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
        }

        //Selection start to end.

        else if (SRow <= ERow)
        {
          if (row == SRow && column >= SCol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
          else if (row == ERow && column <= ECol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
          else if (row > SRow && row < ERow)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
        }

        //Selection end to start.

        else if (SRow >= ERow)
        {
          if (row == SRow && column <= SCol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
          else if (row < SRow && row > ERow)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
          else if (row == ERow && column >= ECol)
          {
            c.setBackground(new Color(57, 105, 138));
            c.setForeground(Color.white);
          }
        }

        //First col is address.

        if (column == 0)
        {
          c.setBackground(Color.black);
          c.setForeground(Color.white);
        }

        return (c);
>>>>>>> parent of a26eaea... Update VHex.java
      }
    }
  }
  
  //Write len bytes at offset to len from current virtual offset pointer.
  
  public void writeV( byte[] b, int off, int len ) throws IOException
  {
<<<<<<< HEAD
    int Pos = off, n = 0; len += off;
    
    //Seek address if outside current address space.
    
    if( getVirtualPointer() > curVra.VEnd ) { seekV( getVirtualPointer() ); }
    
    //Start writing.
    
    while( Pos < len )
    {
      //Write in current offset.
      
      if( super.getFilePointer() >= curVra.Pos && super.getFilePointer() <= curVra.FEnd && curVra.Len > 0 )
      {
        //Number of bytes that can be written in current area.
        
        n = (int)Math.min( ( curVra.FEnd + 1 ) - super.getFilePointer(), len );
        
        super.write( b, Pos, n ); Pos += n;
      }
      
      //Else 0 space. Skip n to Next address.
      
      else
      {
        n = (int)( curVra.VLen - ( super.getFilePointer() - curVra.Pos ) );
        
        if( n < 0 ) { n = len - Pos; }
        
        VAddress += n; Pos += n;
        
        seekV( getVirtualPointer() );
      }
    }
  }
  
  //fire seek event.
  
  @Override public void seek( long Offset ) throws IOException
  {
    while( Events && Trigger ) { EventThread.interrupt(); }
    
    super.seek( Offset ); fireIOEventSeek( new IOEvent( this, Offset, Offset ) );
=======
    TModel.updateData();
>>>>>>> parent of a26eaea... Update VHex.java
  }
  
  //Seek. Same as seek, but is a little faster of a read ahread trick.
  
  @Override public int skipBytes( int n ) throws IOException
  {
    while( Events && Trigger ) { EventThread.interrupt(); }
    
    int b = super.skipBytes( n );
    
    fireIOEventSeek( new IOEvent( this, super.getFilePointer(), super.getFilePointer() ) );
    
    return( b );
  }
  
  //Read and write events.
  
  @Override public int read() throws IOException
  {
    //Trigger writing event.
    
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }
    
    //Start read event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = true; Trigger = true; }
    
    return( super.read() );
  }
  
  @Override public int read( byte[] b ) throws IOException
  {
    //Trigger writing event.
    
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }
    
    //Start read event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = true; Trigger = true; }
    
    return( super.read( b ) );
  }
  
  @Override public int read( byte[] b, int off, int len ) throws IOException
  {
    //Trigger writing event.
    
    while( Events && Trigger && !Read ) { EventThread.interrupt(); }
    
    //Start read event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = true; Trigger = true; }
    
    return( super.read( b, off, len ) );
  }

  
  @Override public void write( int b ) throws IOException
  {
    //Trigger read event.
    
    while( Events && Trigger && Read ) { EventThread.interrupt(); }
    
    //Start write event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = false; Trigger = true; }
    
    super.write( b );
  }
  
  @Override public void write( byte[] b ) throws IOException
  {
    //Trigger read event.
    
    while( Events && Trigger && Read ) { EventThread.interrupt(); }
    
    //Start write event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = false; Trigger = true; }
    
    super.write( b );
  }
  
  @Override public void write( byte[] b, int off, int len ) throws IOException
  {
    //Trigger read event.
    
    while( Events && Trigger && Read ) { EventThread.interrupt(); }
    
    //Start write event tracing.
    
    if( Events && !Trigger ) { TPos = super.getFilePointer(); Read = false; Trigger = true; }
    
    super.write( b, off, len );
  }
  
  //Debug The address mapped memory.
  
  public void Debug()
  {
    String s = "";
    
    for( int i = 0; i < MSize; s += Map.get( i++ ) + "\r\n" );
    
    System.out.println( s );
  }
  
  //Main Event thread.
  
  public void run()
  {
    if( !Running ) //Run once.
    {
      Running = true;
      
      while( Running )
      {
        //If read, or write is triggered.
        
        if( Trigger )
        {
          try
          {
            if( pos == super.getFilePointer() )
            {
              fireIOEvent( new IOEvent( this, TPos, pos ) ); Trigger = false;
            }
            else{ pos = super.getFilePointer(); }
          }
          catch( IOException e ) { e.printStackTrace(); }
        }
        
        //Fire event right away if interrupted, by a different IO event.
        
        try{ Thread.sleep( 100 ); } catch(InterruptedException e) { }
      }
    }
  }
}
