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

  //The hex editor columns.

  String[] Offset = new String[]{ "Offset (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };

  //The table which will update as you scroll through the IO stream.

  JTable data;
  
  //The table model.
  
  AddressModel TModel;
  
  //The currently selected rows and cols in table. Relative to scroll bar.
  
  int SRow = 0, SCol = 0;
  int ERow = 0, ECol = 0;
  
  //The address column is not changeable.

  public class AddressModel extends DefaultTableModel
  {
    AddressModel( Object[][] data, String[] colNames )
    {
      super( data, colNames );
    }

    //The first cell can not be edited.
    
    @Override
    public boolean isCellEditable( int row, int column )
    {
      return( column > 0 );
    }
  }
  
  //The preferred table column size.
  
  class AddressColumnModel extends DefaultTableColumnModel {

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
    //Set the mode for the hex editor component.
    
    if( mode ) { Virtual = true; Offset[0] = "Virtual Address (h)"; }
    
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
    
    TModel = new AddressModel( TData, Offset );

    data = new JTable( TModel, new AddressColumnModel() );
    
    data.createDefaultColumnsFromModel();

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
    
    data.getTableHeader().setReorderingAllowed( false );
    
    //Fill view port height.
    
    data.setFillsViewportHeight( true );
    
    //Do not alow resizing of cells.
    
    data.getTableHeader().setResizingAllowed( false );
    
    //Setup Scroll bar system.

    ScrollBar = new JScrollBar( JScrollBar.VERTICAL, 30, 20, 0, End < 0x7FFFFFFF ? (int) ( ( End + 15 ) / 16 ) : 0x7FFFFFFF );
    
    //Custom selection handling.
    
    data.addMouseListener(new MouseAdapter()
    {
      @Override
      
      public void mousePressed( MouseEvent e )
      {
        SRow = ScrollBar.getValue() + data.rowAtPoint(e.getPoint());
        SCol = data.columnAtPoint(e.getPoint());
        
        ERow = SRow; ECol = SCol;
        
        TModel.fireTableDataChanged();
      }
    });
    
    data.addMouseMotionListener( new MouseMotionAdapter()
    {
      @Override
      
      public void mouseDragged( MouseEvent e )
      {
	    ERow = ScrollBar.getValue() + data.rowAtPoint(e.getPoint());
        ECol = data.columnAtPoint(e.getPoint());
        
        //Automatically scroll while selecting bytes.
        
        if( e.getY() > ( data.getHeight() - 70 ) )
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
        long CurPos = ( RelPos + ScrollBar.getValue() ) * 16;
        
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
		
        //Read data at scroll position.

        try
        {
          //If offset mode use offset seek.
          
          if( !Virtual ) { IOStream.seek( CurPos ); }

          //If Virtual use Virtual map seek.

          else { IOStream.seekV( CurPos ); }
        }
        catch( java.io.IOException e1 ) {}

        //Number of bytes to be read. Note this should be updated to number of rows that can be rendered in drawing space.
        
        byte[] b = new byte[ data.getRowCount() * 16 ];
        
        try
        {
          //If offset mode use unmaped read.

          if( !Virtual ) { IOStream.read( b ); }

          //If Virtual then use Virtual map read.

          else { IOStream.readV( b ); }
        }
        catch( java.io.IOException e1 ) {}
        
        //The current byte in IO stream.

        int pos = 0;
        
        //Update table cells with the new bytes at address position.
        
        for( int rn = 0; rn < TData.length; rn++ )
        {
          //The curent offset at curent row per 16 offset.

          TModel.setValueAt( "0x" + String.format( "%1$016X", CurPos ), rn, 0 );

          //Fill each row.
          
          for( int i = 1; i < 17; i++, pos++, CurPos++ )
          {
            TModel.setValueAt( ( CurPos < End ) ? String.format( "%1$02X", b[ pos ] ) : "??", rn, i );
          }
          
          TModel.fireTableDataChanged();
        }
      }
    }

    ScrollBar.addAdjustmentListener( new Scroll( ) );
    
    //Custom table selection rendering.
    
    data.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
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
    super.add( data.getTableHeader(), BorderLayout.PAGE_START );
    super.add( data, BorderLayout.CENTER );
    super.add( ScrollBar, BorderLayout.EAST );

    ScrollBar.setValue( 0 );
  }
}
