import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

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

        //Number of bytes to be read.        
        
        byte[] b = new byte[ 0x400 ];
        
        try
        {
          //If offset mode use unmaped read.

          if( !Virtual ) { IOStream.read( b ); }

          //If Virtual use Virtual map read.

          else { IOStream.readV( b ); }
        }
        catch( java.io.IOException e1 ) {}

        //16 bytes per line. Plus an additional for the address.
        
        String[][] TData = new String[ (int) ( ( b.length + 1 ) / 16 ) ][ 17 ];
        
        //The current row number.

        int pos = 0;
        
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

        data.setModel( tabel );

        data.getColumnModel().getColumn( 0 ).setPreferredWidth( 160 );

        data.getColumnModel().setColumnSelectionAllowed( true );
        
        data.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

        for( int i = 1; i < 17; i++ ) { data.getColumnModel().getColumn( i ).setPreferredWidth( 20 ); }

        data.getTableHeader().setReorderingAllowed( false );

        data.setFillsViewportHeight( true );
      }
    }

    ScrollBar.addAdjustmentListener( new Scroll( ) );

    super.setLayout( new BorderLayout( ) );
    super.add( data.getTableHeader(), BorderLayout.PAGE_START );
    super.add( data, BorderLayout.CENTER );
    super.add( ScrollBar, BorderLayout.EAST );

    ScrollBar.setValue( 0 );
  }
}
