import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class VHex extends JComponent
{
  //The file system stream reference that will be used.

  RandomAccessFileV IOStream;

  //The end of the data stream.

  long End = 0;

  //The curent position in IO stream.
  
  long CurPos = 0;

  //The table which will update as you scroll through the IO stream.

  JTable tdata;
  
  //The table model.
  
  AddressModel TModel;
  
  //The currently selected rows and cols in table. Relative to scroll bar.
  
  int SRow = 0, SCol = 0;
  int ERow = 0, ECol = 0;
  
  //The main hex edior display.
  
 class AddressModel extends AbstractTableModel
 {
    private String[] Offset = new String[]{ "Offset (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };

    //Byte buffer betwean io stream.

    private byte[] data = new byte[ 0x4A0 ];
    
    //Divide into rows of 16 offsets.
    
    private int RowLen = 16;
    
    //If virtual mode.
    
    public AddressModel( boolean mode ) { if( mode ) { Offset[ 0 ] = "Virtual Address (h)"; } }
    
    //Get number of columns.

    public int getColumnCount() { return( Offset.length ); }

    //Get number of rows in Display area.

    public int getRowCount() { return( ( tdata.getHeight() / tdata.getRowHeight() ) + 1 ); }

    //Get the column.

    public String getColumnName( int col ) { return( Offset[ col ] ); }

    //The address col and byte values.

    public Object getValueAt( int row, int col )
    {
      //First col is address.
      
      if( col == 0 ) { return( "0x" + String.format( "%1$016X", CurPos + ( row * 16 ) ) ); }
      
      //Else byte to hex.
      
      if( ( ( row * RowLen ) + ( col - 1 ) ) < data.length ){ return( String.format( "%1$02X", data[ ( row * RowLen ) + ( col - 1 ) ] ) ); }
      
      else{ return("??"); }
    }
    
    //JTable uses this method to determine the default renderer/editor for each cell.
    
    public Class getColumnClass( int c ) { return( getValueAt(0, c).getClass() ); }

    //First column is not editbale as it is the address.
    
    public boolean isCellEditable( int row, int col ) { return( col >= 1 ); }

    //Do not fire if updating byte buffer. Seting values writes directly to the IO stream.
    
    public void setValueAt(Object value, int row, int col)
    {
      data[ ( row * RowLen ) + col ] = (byte)Integer.parseInt( (String)value, 16 );
      
      //Write the new byte value to stream.
      
      //Update table.
      
      fireTableCellUpdated( row, col );
    }
    
    //Update table data.
    
    public void updateData()
    {
	  //Read data at scroll position.

      try
      {
        //If offset mode use offset seek.
        
        if( !Virtual )
        {
	      IOStream.seek( CurPos );
	      IOStream.read( data );
	    }
        
        //If Virtual use Virtual map seek.
        
        else
        {
		  IOStream.seekV( CurPos );
		  IOStream.readV( data );
	}
      }
      catch( java.io.IOException e1 ) {}
      
      fireTableDataChanged();
	}
  }
  
  //The preferred table column size.
  
  class AddressColumnModel extends DefaultTableColumnModel
  {
    public void addColumn(TableColumn c)
    {
	  //Address column.
	  
	  if( super.getColumnCount() == 0 )
	  {
	    c.setPreferredWidth( 130 );
	  }
	  
	  //Byte value columns.
	  
	  else
	  {
	    c.setPreferredWidth( 20 );
	  }
	  
      super.addColumn( c );
    }
  }
  
  //Remove the tables default selection methods.
  
  class NullSelectionModel implements ListSelectionModel
  {
    public boolean isSelectionEmpty() { return( true ); }
    public boolean isSelectedIndex( int index ) { return( false ); }
    public int getMinSelectionIndex() { return( -1 ); }
    public int getMaxSelectionIndex() { return( -1 ); }
    public int getLeadSelectionIndex() { return( -1 ); }
    public int getAnchorSelectionIndex() { return( -1 ); }
    public void setSelectionInterval( int index0, int index1 ) { }
    public void setLeadSelectionIndex( int index ) { }
    public void setAnchorSelectionIndex( int index ) { }
    public void addSelectionInterval( int index0, int index1 ) { }
    public void insertIndexInterval( int index, int length, boolean before ) { }
    public void clearSelection() { }
    public void removeSelectionInterval( int index0, int index1 ) { }
    public void removeIndexInterval( int index0, int index1 ) { }
    public void setSelectionMode( int selectionMode ) { }
    public int getSelectionMode() { return( SINGLE_SELECTION ); }
    public void addListSelectionListener( ListSelectionListener lsl ) { }
    public void removeListSelectionListener( ListSelectionListener lsl ) { }
    public void setValueIsAdjusting( boolean valueIsAdjusting ) { }
    public boolean getValueIsAdjusting() { return( false ); }
  }

  //The hex editors scroll bar.

  JScrollBar ScrollBar;
  
  //Enable relative scrolling for files larger than 4Gb.
  
  boolean Rel = false;
  
  //Position that is relative to scroll bar position.
  
  long RelPos = 0;

  //Virtual mode, or offset mode.

  boolean Virtual = false;
  
  //If no mode setting then assume offset mode.

  public VHex( RandomAccessFileV f ) { this( f, false ); }

  //Initialize the hex UI component. With file system stream.

  public VHex( RandomAccessFileV f, boolean mode )
  { 
    Virtual = mode;  
	
    //Reference the file stream.

    IOStream = f;
	
    //Inilize a small table.
	
    String[][] TData = new String[ 16 ][ 17 ];
    
    //Create table model.
    
    for( int rn = 0; rn < TData.length; rn++ )
    {
      for( int i = 0; i < 17; i++ )
      {
        TData[ rn ][ i ]= "??";
      }
    }
    
    TModel = new AddressModel( mode );

    tdata = new JTable( TModel, new AddressColumnModel() );
    
    tdata.createDefaultColumnsFromModel();
    
    tdata.setSelectionModel( new NullSelectionModel() );

    //The length of the stream.

    try
    {
      //If offset mode then end is the end of the stream.
      
      if( !Virtual )
      {
        End = IOStream.length();
		  
        //Enable relative scrolling if the data length is outside the scroll bar range.

        if( End > 0x7FFFFFFF ) { Rel = true; }
      }

      //Else the last 64 bit virtual address. Thus set relative scrolling.
      
      else { Rel = true; End = 0x7FFFFFFFFFFFFFFFL; }
    }
    catch( java.io.IOException e ) { }

    //Columns can not be re-arranged.
    
    tdata.getTableHeader().setReorderingAllowed( false );
    
    //Fill view port height.
    
    tdata.setFillsViewportHeight( true );
    
    //Do not alow resizing of cells.
    
    tdata.getTableHeader().setResizingAllowed( false );
    
    //Setup Scroll bar system.

    ScrollBar = new JScrollBar( JScrollBar.VERTICAL, 30, 20, 0, End < 0x7FFFFFFF ? (int) ( ( End + 15 ) / 16 ) : 0x7FFFFFFF );
    
    //Custom selection handling.
    
    tdata.addMouseListener(new MouseAdapter()
    {
      @Override
      
      public void mousePressed( MouseEvent e )
      {
        SRow = ScrollBar.getValue() + tdata.rowAtPoint(e.getPoint());
        SCol = tdata.columnAtPoint(e.getPoint());
        
        ERow = SRow; ECol = SCol;
        
        TModel.fireTableDataChanged();
      }
    });
    
    tdata.addMouseMotionListener( new MouseMotionAdapter()
    {
      @Override
      
      public void mouseDragged( MouseEvent e )
      {
	    ERow = ScrollBar.getValue() + tdata.rowAtPoint(e.getPoint());
        ECol = tdata.columnAtPoint(e.getPoint());
        
        //Automatically scroll while selecting bytes.
        
        if( e.getY() > ( tdata.getHeight() - 70 ) )
        {
		  ScrollBar.setValue( Math.min( ScrollBar.getValue() + 1, 0x7FFFFFFF ) );
		}
		else if( e.getY() < 0 )
        {
		  ScrollBar.setValue( Math.max( ScrollBar.getValue() - 1, 0 ) );
		}
		
		//Force the table to rerender cells.
        
        TModel.fireTableDataChanged();
      }
    });
    
    //As we scroll update the table data. As it would be insane to graphically render large files in hex.

    class Scroll implements AdjustmentListener
    {
      public void adjustmentValueChanged( AdjustmentEvent e )
      {
        CurPos = ( RelPos + ScrollBar.getValue() ) * 16;
        
        //If relative scrolling.
        
        if( Rel )
        {
          if( ScrollBar.getValue() > 1879048191 )
          {
			RelPos = Math.min( RelPos + ( ScrollBar.getValue() - 1879048191 ), 0x7FFFFFFF80000000L );
			if( RelPos < 0x7FFFFFFF80000000L ) { ScrollBar.setValue( 1879048191 ); }
		  }
		  
		  else if( ScrollBar.getValue() < 268435456 )
		  {
			RelPos = Math.max( RelPos - ( 268435456 - ScrollBar.getValue() ), 0 );
			if( RelPos > 0 ) { ScrollBar.setValue( 268435456 ); }
		  }
        }
        
        TModel.updateData();
      }
    }

    ScrollBar.addAdjustmentListener( new Scroll( ) );
    
    //Custom table selection rendering.
    
    tdata.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
    {
      @Override
      
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
      {  
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        row += RelPos + ScrollBar.getValue();
        
        //Alternate shades between rows.
        
        if ( row % 2 == 0 )
        {
          c.setBackground( Color.white );
          c.setForeground( Color.black );
        }
        else
        {
          c.setBackground( new Color(242, 242, 242) );
          c.setForeground( Color.black );
        }
        
        //If selection is in same row
        
        if( SRow == ERow && row == SRow )
        {
		  if( SCol > ECol && column >= ECol && column <= SCol )
		  {
            c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		  else if( column <= ECol && column >= SCol )
		  {
            c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
	    }
        
        //Selection start to end.
        
        else if ( SRow <= ERow )
        {
          if ( row == SRow && column >= SCol )
          {
            c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		  else if ( row == ERow && column <= ECol )
		  {
		    c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		  else if ( row > SRow && row < ERow )
		  {
			c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		}
		
		//Selection end to start.
		
        else if ( SRow >= ERow )
        {
          if ( row == SRow && column <= SCol )
          {
            c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		  else if ( row < SRow && row > ERow )
		  {
			c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		  else if ( row == ERow && column >= ECol )
		  {
		    c.setBackground(new Color (57, 105, 138));
            c.setForeground(Color.white);
		  }
		}
		
       //First col is address.

        if( column == 0 )
        {
          c.setBackground(Color.black);
          c.setForeground(Color.white);
        }
        
        return( c );
      }
    });
    
    //Add everything to main component.

    super.setLayout( new BorderLayout( ) );
    super.add( tdata.getTableHeader(), BorderLayout.PAGE_START );
    super.add( tdata, BorderLayout.CENTER );
    super.add( ScrollBar, BorderLayout.EAST );
  }
}
