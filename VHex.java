import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class VHex extends JComponent
{
  //The file system stream reference that will be used.

  RandomAccessFileV IOStream;

  //The end of the file.

  long End = 0;

  //The hex editor columns.

  String[] OffsetMode = new String[]{ "Offset (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };
  String[] VirtualMode = new String[]{ "Virtual Address (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };

  //The table which will update as you scroll through the IO stream.

  JTable data = new JTable( );
  
  //End and start byte selection.
  
  long Sel_Start = 0x0, Sel_End = 0x0;
  
  //This is used to stop the selection from changing address by running the selection event when updating the selected bytes while scrolling.
  
  boolean Move = false;

  //The address is not changeable.

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

  //The hex editors scroll bar.

  JScrollBar ScrollBar;

  //Virtual mode, or offset mode.

  boolean Virtual = false;
  
  //If no mode setting then assume file offset mode.

  public VHex( RandomAccessFileV f ) { this( f, false ); }

  //Initialize the hex UI component. With file system stream.

  public VHex( RandomAccessFileV f, boolean mode )
  {
    //Set the mode for the hex editor component.

    Virtual = mode;

    //Reference the file stream.

    IOStream = f;

    //The length of the file.

    try
    {
      //If file offset mode the end is the end of the file.
      
      if( !Virtual ) { End = IOStream.length(); }

      //Else the last 64 bit address. Because of scroll bar limit smaller size is used.
      //Might create A custom scroll bar for this use.

      else { End = 0x0FFFFFFF; }
    }
    catch( java.io.IOException e ) { }

    //Setup the scroll bar.

    ScrollBar = new JScrollBar( JScrollBar.VERTICAL, 30, 20, 0, (int) ( ( End + 15 ) / 16 ) );
    
    //As we scroll update the table data. As it would be insane to graphically render large files in hex.

    class Scroll implements AdjustmentListener
    {
      public void adjustmentValueChanged( AdjustmentEvent e )
      {
        //Seek the current position at scroll bar position.

        long CurPos = e.getValue() * 16;

        try
        {
          //If offset mode use offset seek.
          
          if( !Virtual ) { IOStream.seek( CurPos ); }

          //If Virtual use Virtual map seek.

          else { IOStream.seekV( CurPos ); }
        }
        catch( java.io.IOException e1 ) {}

        //Number of bytes to be read. Note this should be updated to number of rows that can be rendered in draing space.
        
        byte[] b = new byte[ 0x400 ];
        
        try
        {
          //If offset mode use unmaped read.

          if( !Virtual ) { IOStream.read( b ); }

          //If Virtual then use Virtual map read.

          else { IOStream.readV( b ); }
        }
        catch( java.io.IOException e1 ) {}

        //16 bytes per line. Plus an additional for the address.
        
        String[][] TData = new String[ (int) ( ( b.length + 1 ) / 16 ) ][ 17 ];
        
        //The current row number.

        int pos = 0;
        
        //Table address position is changing.
        
        Move = true;
        
        //Update table cells with the new bytes at address position.
        
        for( int rn = 0; rn < TData.length; rn++ )
        {
          //The curent offset at curent row per 16 offset.

          TData[ rn ][ 0 ] = "0x" + String.format( "%1$016X", CurPos );

          //Fill each row.
          
          for( int i = 1; i < 17; i++, pos++, CurPos++ )
          {
            if( CurPos < End ){ TData[ rn ][ i ]= String.format( "%1$02X", b[ pos ] ); } else { TData[ rn ][ i ]= "??"; }
          }
        }
        
        AddressModel tabel = new AddressModel( TData, Virtual ? VirtualMode : OffsetMode );

        data.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        data.setColumnSelectionAllowed(true);
        data.setRowSelectionAllowed(true);

        data.setModel( tabel );
        
        //The selected bytes. If any.
        
        CurPos = e.getValue() * 16;
        
        //Start Address.
        
        int SRow = (int)( ( Sel_Start - CurPos ) / 16 );
        int SCol = (int)( Sel_Start % 16 );
        
        //If negative rows select row 0.
        
        if( SRow < 0 ){ SRow = 0; }
        
        //End Address position.
        
        int ERow = (int)( ( Sel_End - CurPos ) / 16 );
        int ECol = (int)( Sel_End % 16 );
        
        //Max number of rows in table.
        
        if( ERow >= 63 ) { ERow = 63; }
        
        //If within the tabel area.
        
        if( ERow > 0 && SRow <= 63 )
        {
          data.changeSelection( SRow, SCol, false, false ); //Start
          data.changeSelection( ERow, ECol, false, true );  //End.
	    }
        

        data.getColumnModel().getColumn( 0 ).setPreferredWidth( 160 );

        for( int i = 1; i < 17; i++ ) { data.getColumnModel().getColumn( i ).setPreferredWidth( 20 ); }

        data.getTableHeader().setReorderingAllowed( false );

        data.setFillsViewportHeight( true );
        
        Move = false;
      }
    }

    ScrollBar.addAdjustmentListener( new Scroll( ) );

    ListSelectionModel cellSelectionModel = data.getSelectionModel();
    cellSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    cellSelectionModel.addListSelectionListener(new ListSelectionListener()
    {
      public void valueChanged(ListSelectionEvent e)
      {
		if( Move == false )
		{
		  long Base_Address = ScrollBar.getValue() * 16;
		  
          int[] selectedRow = data.getSelectedRows();
          int[] selectedColumns = data.getSelectedColumns();
          
          Sel_Start = Base_Address + ( ( selectedRow[ 0 ] * 16 ) + selectedColumns[ 0 ] );
          Sel_End = Base_Address + ( ( selectedRow[ selectedRow.length - 1 ] * 16 ) + selectedColumns[ selectedColumns.length - 1 ] );
	    }
      }
    });

    super.setLayout( new BorderLayout( ) );
    super.add( data.getTableHeader(), BorderLayout.PAGE_START );
    super.add( data, BorderLayout.CENTER );
    super.add( ScrollBar, BorderLayout.EAST );

    ScrollBar.setValue( 0 );
  }
}
