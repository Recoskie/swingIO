import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class VHex extends JComponent implements IOEventListener, MouseWheelListener
{
  //The file system stream reference that will be used.

  private RandomAccessFileV IOStream;
  
  //Convert IO stream into row or col position.
  
  private long getRowPos()
  {
    try { return ( ( Virtual ? IOStream.getVirtualPointer() : IOStream.getFilePointer() ) & 0xFFFFFFFFFFFFFFF0L ); } catch ( java.io.IOException e ) { }
    return( -1 );
  }
  
  private long getColPos()
  {
    try { return ( ( Virtual ? IOStream.getVirtualPointer() : IOStream.getFilePointer() ) & 0x0F ); } catch ( java.io.IOException e ) { }
    return( -1 );
  }

  //Monitor when scrolling.
  
  private boolean isScrolling = false;

  //The table which will update as you scroll through the IO stream.

  private JTable tdata;
  
  //The table model.

  AddressModel TModel;

  //Number of rows in draw space.

  private int TRows = 0;

  //The currently selected rows and cols in table. Relative to scroll bar.

  private long SRow = 0, SCol = 0;
  private long ERow = 0, ECol = 0;
  
  //Selection Color.
  
  private Color SelectC = new Color(57, 105, 138);
  
  //Byte buffer between io stream. Updated based on number of rows that can be displayed.

  private byte[] data = new byte[0];
  private boolean[] udata = new boolean[0]; //Bytes that could not be read.
  
  //A modified scroll bar for VHex. Allows for a much larger scroll area of very big data.

  private LongScrollBar ScrollBar;

  //Virtual mode, or offset mode.

  private boolean Virtual = false;

  //The main hex editor display.

  private class AddressModel extends AbstractTableModel
  {
    private String[] Offset = new String[] { "Offset (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };

    //If virtual mode.

    public AddressModel( boolean mode ) { if (mode) { Offset[0] = "Virtual Address (h)"; } }

    //Get number of columns.

    public int getColumnCount() { return ( Offset.length ); }

    //Get number of rows in Display area.

    public int getRowCount() { return ( TRows ); }

    //Get the column.

    public String getColumnName( int col ) { return ( Offset[ col ] ); }

    //The address col and byte values.

    public Object getValueAt( int row, int col )
    {
      //Calculate position.
      
      row <<= 4; col -= 1;
      
      //First col is address.
      
      if ( col < 0 ) { return ( "0x" + String.format( "%1$016X", ScrollBar.getRelValue() + row ) ); }

      //Byte to hex.
      
      if ( !udata[row + col] )
      {
        return ( String.format( "%1$02X", data[ row + col ] ) );
      }
      else
      {
        return("??");
      }
    }

    //JTable uses this method to determine the default render/editor for each cell.

    public Class getColumnClass( int c ) { return ( getValueAt( 0, c ).getClass() ); }

    //First column is not editable as it is the address.

    public boolean isCellEditable( int row, int col )
    {
      return ( !TModel.getValueAt( row, col ).equals("??") && col >= 1 );
    }

    //Setting values writes directly to the IO stream.

    public void setValueAt( Object value, int row, int col )
    {
      int b = Integer.parseInt((String) value, 16);

      data[ ( row << 4 ) + ( col - 1 ) ] = (byte)b;

      //Write the new byte value to stream.

      try { if ( Virtual ) { IOStream.writeV(b); } else { IOStream.write(b); } } catch (java.io.IOException e1) {}
    }

    //Update table data.

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

          //seek scroll bar position.
          
          IOStream.seek( ScrollBar.getRelValue() );

          rd = IOStream.read( data ); rd = rd < 0 ? 0 : rd;

          //Undefined bytes only happen at end of file. Or failed to read file.

          for( int i = rd; i < data.length; i++ ) { udata[i] = true; }

          //back to original pos.

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
            //seek scroll bar position.
          
            IOStream.seekV( ScrollBar.getRelValue() + pos );

            //Number of bytes that can be read before no data.

            rd = IOStream.readV( data, pos, data.length - pos ); rd = rd < 0 ? 0 : rd; pos += rd;

            //Calculate undefined space to next address.

            end = (int)(IOStream.nextV() - IOStream.getVirtualPointer()) + pos;

            if( end > data.length || end <= 0 ) { end=data.length; }

            //space that is undefined.

            for( int i = pos; i < end; i++ ) { udata[i] = true; } pos = end;
          }

          //back to original pos.

          IOStream.seekV( t );
          
          IOStream.Events = true;
        }
      }
      catch (java.io.IOException e1) {}

      fireTableDataChanged();
    }
  }
  
  //Modified scrollbar class to Handel the full 64 bit address space.
  //Virtual space is treated as unsigned. Except physical file positions.
  
  private class LongScrollBar extends JScrollBar
  {
    private long End = 0, VisibleEnd = 0, Pos = 0, RPos = 0;
    private int RelUp = 0x70000000, RelDown = 0x10000000;
    private int ov = 0;
    
    public LongScrollBar(int orientation, int value, int visible, int minimum, long maximum)
    {
      super( orientation, value, visible, minimum, Long.compareUnsigned( maximum, 0x7FFFFFF0 ) > 0 ? 0x7FFFFFF0 : (int) ( ( maximum + 15 ) & 0x7FFFFFF0 ) );
      End = maximum; VisibleEnd = End - visible;
    }
    
    @Override public void setValue( int v )
    {
      isScrolling = true;
      
      if( tdata.isEditing() ) { tdata.getCellEditor().stopCellEditing(); }
      
      if( Long.compareUnsigned( VisibleEnd, 0x7FFFFFF0 ) > 0 )
      {
        if( ov < v )
        {
          Pos += ( v - ( 0x7FFFFFF0 - RelUp ) ); v = RelUp;
        }
        
        else if( ov > v )
        {
          Pos -= ( RelDown - v ); v = RelDown;
        }
      }
      
      RPos = v + Pos; ov = v;
      
      super.setValue( v & 0x7FFFFFF0 );
      TModel.updateData();
      
      isScrolling = false;
    }
    
    @Override public void setVisibleAmount( int v )
    {
      super.setVisibleAmount( v ); VisibleEnd = End - v; setValue( Pos + super.getValue() );
    }
    
    public void setValue( long v )
    {
      isScrolling = true;
      
      RPos = v;
      
      if( Long.compareUnsigned( v, RelUp ) > 0 ) { Pos = v - RelUp; v = RelUp; }
      
      if( Long.compareUnsigned( v, VisibleEnd ) > 0 ){ v = VisibleEnd; }
      
      super.setValue( ( (int) v ) & 0x7FFFFFF0 ); TModel.updateData();
      
      isScrolling = false;
    }
    
    public long getRelValue() { return( RPos ); }
  }

  //The table column size.

  private class AddressColumnModel extends DefaultTableColumnModel
  {
    int pixelWidth = 0;
  
    public void addColumn(TableColumn c)
    {
      //Init width.
      
      if( pixelWidth <= 0 )
      {
        java.awt.FontMetrics fm = tdata.getFontMetrics(tdata.getFont());
        pixelWidth = fm.stringWidth("C");
      }
      
      //Address column.
      
      if (super.getColumnCount() == 0) { c.setMinWidth( pixelWidth * 18 + 2 ); c.setMaxWidth( pixelWidth * 18 + 2 ); }

      //Byte value columns.

      else { c.setMinWidth( pixelWidth * 2 + 3 ); c.setMaxWidth( pixelWidth * 2 + 3 ); }

      //Add column.

      super.addColumn(c);
    }
  }

  //A simple cell editor for my hex editor.

  private class CellHexEditor extends DefaultCellEditor
  {
    final JTextField textField; //The table text component.

    private int pos = 0; //Character position in cell.

    private int Row = 0, Col = 0; //Current cell.

    boolean CellMove = false; //Moving cells.

    private class HexDocument extends PlainDocument
    {
      @Override public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException
      {
        //Validate hex input.

        char c = text.toUpperCase().charAt(0);

        if (c >= 0x41 && c <= 0x46 || c >= 0x30 && c <= 0x39)
        {
          pos = offset + 1;
          super.replace(offset, length, text, attrs);
          UpdatePos();
        }

        textField.select(pos, pos + 1);
      }
    }

    //Move left, or right. Cursor position.

    private void UpdatePos()
    {
      //Move the editor while entering hex.

      if (pos < 0)
      {
        Col -= 1;

        if (Col <= 0)
        {
          Col = 16;

          if (Row == 0)
          {
            ScrollBar.setValue(ScrollBar.getValue() - 1);
          }
          else
          {
            Row -= 1;
          }
        }

        tdata.editCellAt( Row, Col ); tdata.getEditorComponent().requestFocus();
        pos = 1; CellMove = true; return;
      }

      if (pos > 1)
      {
        Col += 1;

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

        tdata.editCellAt( Row, Col ); tdata.getEditorComponent().requestFocus();
        pos = 0; CellMove = true; return;
      }

      //Select the current hex digit user is editing.

      CellMove = false;
    }

    public CellHexEditor()
    {
      super(new JTextField());
      textField = (JTextField) getComponent();

      textField.addFocusListener(new FocusAdapter()
      {
        @Override public void focusGained(FocusEvent e)
        {
          if (!CellMove)
          {
            pos = Math.max(0, textField.getCaretPosition() - 1);
          }
          textField.select(pos, pos + 1);
          TModel.fireTableDataChanged();
        }
      });

      textField.addMouseListener(new MouseAdapter()
      {
        @Override public void mousePressed(MouseEvent e)
        {
          pos = Math.max(0, textField.getCaretPosition() - 1);
          textField.select(pos, pos + 1);
        }

        @Override public void mouseReleased(MouseEvent e)
        {
          pos = Math.max(0, textField.getCaretPosition() - 1);
          textField.select(pos, pos + 1);
        }

        @Override public void mouseClicked(MouseEvent e)
        {
          pos = Math.max(0, textField.getCaretPosition() - 1);
          textField.select(pos, pos + 1);
        }
      });

      textField.addKeyListener(new KeyListener()
      {
        public void keyPressed(KeyEvent e)
        {
          int c = e.getKeyCode();

          if (c == e.VK_LEFT) { pos -= 1; } else if (c == e.VK_RIGHT) { pos += 1; }

          UpdatePos();
        }

        public void keyReleased(KeyEvent e)
        {
          textField.select(pos, pos + 1);
        }

        public void keyTyped(KeyEvent e) {}
      });

      textField.setDocument(new HexDocument());
    }

    @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
      final JTextField textField = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);

      Row = row; Col = column; return (textField);
    }
  }

  //Only recalculate number of table rows on resize. Speeds up table rendering.

  private class CalcRows extends ComponentAdapter
  {
    public void componentResized(ComponentEvent e)
    {
      TRows = ( ( tdata.getHeight() - 8 ) / tdata.getRowHeight()) + 1;
      data = java.util.Arrays.copyOf( data, TRows * 16 );
      ScrollBar.setVisibleAmount( TRows << 4 );
    }
  }
  
  //If no mode setting then assume offset mode.

  public VHex(RandomAccessFileV f) { this(f, false); }

  //Initialize the hex UI component. With file system stream.

  public VHex(RandomAccessFileV f, boolean mode)
  {
    //Register this component to update on IO system calls.
    
    f.addIOEventListener( this );
    
    //Row resize calculation.
    
    super.addComponentListener( new CalcRows() );

    Virtual = mode;

    //Reference the file stream.

    IOStream = f;

    TModel = new AddressModel( mode );

    tdata = new JTable( TModel, new AddressColumnModel() );

    tdata.createDefaultColumnsFromModel();

    //Columns can not be re-arranged.

    tdata.getTableHeader().setReorderingAllowed( false );

    //Do not allow resizing of cells.

    tdata.getTableHeader().setResizingAllowed( false );

    //Set the table editor.

    tdata.setDefaultEditor( String.class, new CellHexEditor() );

    //Setup Scroll bar system.

    try { ScrollBar = new LongScrollBar(JScrollBar.VERTICAL, 16, 0, 0, Virtual ? 0xFFFFFFFFFFFFFFFFL : IOStream.length() ); } catch (java.io.IOException e) {}
    
    ScrollBar.setUnitIncrement( 16 );

    //Custom selection handling.

    tdata.addMouseListener(new MouseAdapter()
    {
      @Override public void mousePressed(MouseEvent e)
      {
        if( TModel.getValueAt( tdata.rowAtPoint(e.getPoint()), tdata.columnAtPoint(e.getPoint()) ).equals("??") ) { return; }
        try
        {
          if( !Virtual )
          {
            IOStream.seek( ( ScrollBar.getRelValue() + ( tdata.rowAtPoint(e.getPoint()) << 4 ) ) + ( tdata.columnAtPoint(e.getPoint()) - 1 ) );
          }
          else
          {
            IOStream.seekV( ( ScrollBar.getRelValue() + ( tdata.rowAtPoint(e.getPoint()) << 4 ) ) + ( tdata.columnAtPoint(e.getPoint()) - 1 ) );
          }
        }
        catch( java.io.IOException e1 ) {}
      }
    });

    tdata.addMouseMotionListener(new MouseMotionAdapter()
    {
      @Override public void mouseDragged(MouseEvent e)
      {
        //Automatically scroll while selecting bytes.

        if (e.getY() > tdata.getHeight())
        {
          ScrollBar.setValue( ScrollBar.getValue() + 64 );
          ERow = ScrollBar.getRelValue() + ( ( TRows - 1 ) << 4 );
        }
        else if ( e.getY() < 0 )
        {
          ScrollBar.setValue( ScrollBar.getValue() - 64 );
          ERow = ScrollBar.getRelValue();
        }
        else
        {
          ERow = ScrollBar.getRelValue() + ( tdata.rowAtPoint(e.getPoint()) << 4 );
          ECol = tdata.columnAtPoint(e.getPoint());
        }
        
        TModel.fireTableDataChanged();
      }
    });

    //Custom table selection rendering.

    tdata.setDefaultRenderer( Object.class, new DefaultTableCellRenderer()
    {
      @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int column)
      {
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, r, column);

        long row = ( r << 4 ) + ScrollBar.getRelValue();

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
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
          else if (column <= ECol && column >= SCol)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
        }

        //Selection start to end.

        else if (SRow <= ERow)
        {
          if (row == SRow && column >= SCol)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
          else if (row == ERow && column <= ECol)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
          else if (row > SRow && row < ERow)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
        }

        //Selection end to start.

        else if (SRow >= ERow)
        {
          if (row == SRow && column <= SCol)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
          else if (row < SRow && row > ERow)
          {
            c.setBackground( SelectC );
            c.setForeground(Color.white);
          }
          else if (row == ERow && column >= ECol)
          {
            c.setBackground( SelectC );
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
      }
    });
    
    //Add scroll wheal handler.
    
    tdata.addMouseWheelListener(this);

    //Add everything to main component.

    super.setLayout(new BorderLayout());
    super.add(tdata.getTableHeader(), BorderLayout.PAGE_START);
    super.add(tdata, BorderLayout.CENTER);
    super.add(ScrollBar, BorderLayout.EAST);

    TModel.updateData();
  }
  
  //Adjust scroll bar on scroll wheal.
  
  @Override public void mouseWheelMoved(MouseWheelEvent e)
  {
    if( tdata.isEditing() ) { tdata.getCellEditor().stopCellEditing(); }
    
    ScrollBar.setValue( Math.max( 0, ScrollBar.getRelValue() + ( e.getUnitsToScroll() << 4 ) ) );
  }
  
  //On seeking a new position in stream.
  
  public void onSeek( IOEvent e )
  {
    SelectC = new Color( 57, 105, 138 );
    
    if( !isScrolling )
    {
      //The editors row position.
      
      long CRow = ScrollBar.getRelValue() & 0xFFFFFFFFFFFFFFF0L;
      
      //The IO stream position.
      
      if(!Virtual)
      {
        SRow = e.SPos() & 0x7FFFFFFFFFFFFFF0L; SCol = ( e.SPos() & 0xF ) + 1;
      }
      else
      {
        SRow = e.SPosV() & 0xFFFFFFFFFFFFFFF0L; SCol = ( e.SPosV() & 0xF ) + 1;
      }
      
      ECol = SCol; ERow = SRow;
      
      //Only update scroll bar, and data if on outside of the editor.
      
      if( SRow < CRow || SRow >= ( CRow + ( TRows << 4 ) ) )
      {
        ScrollBar.setValue( SRow );
      }
      
      //Else fire table rendering changed for selection.
      
      else
      {
        TModel.fireTableDataChanged();
      }
    }
  }
  
  //On Reading bytes in stream.
  
  public void onRead( IOEvent e )
  {
    SelectC = new Color( 33, 255, 33 );
    
    if(!Virtual)
    {
      SRow = e.SPos() & 0x7FFFFFFFFFFFFFF0L; SCol = ( e.SPos() & 0xF ) + 1;
      ERow = e.EPos() & 0x7FFFFFFFFFFFFFF0L; ECol = ( e.EPos() & 0xF );
    }
    else
    {
      SRow = e.SPosV() & 0xFFFFFFFFFFFFFFF0L; SCol = ( e.SPosV() & 0xF ) + 1;
      ERow = e.EPosV() & 0xFFFFFFFFFFFFFFF0L; ECol = ( e.EPosV() & 0xF );
    }
    
    //The editors row position.
      
    long CRow = ScrollBar.getRelValue() & 0x7FFFFFFFFFFFFFF0L;
      
    //Only update scroll bar, and data if on outside of the editor.
    
    if( SRow <= CRow ) { ScrollBar.setValue( SRow ); }
      
    if( ERow >= ( CRow + ( TRows << 4 ) ) ) { ScrollBar.setValue( ERow - ( ( TRows - 1 ) << 4 ) ); }
    
    TModel.fireTableDataChanged();
  }
  
  //On writing bytes in stream.
  
  public void onWrite( IOEvent e )
  {
    SelectC = new Color( 255, 33, 33 );
    
    if(!Virtual)
    {
      SRow = e.SPos() & 0x7FFFFFFFFFFFFFF0L; SCol = ( e.SPos() & 0xF ) + 1;
      ERow = e.EPos() & 0x7FFFFFFFFFFFFFF0L; ECol = ( e.EPos() & 0xF );
    }
    else
    {
      SRow = e.SPosV() & 0xFFFFFFFFFFFFFFF0L; SCol = ( e.SPosV() & 0xF ) + 1;
      ERow = e.EPosV() & 0xFFFFFFFFFFFFFFF0L; ECol = ( e.EPosV() & 0xF ); 
    }
    
    //The editors row position.
      
    long CRow = ScrollBar.getRelValue() & 0xFFFFFFFFFFFFFFF0L;
      
    //Only update scroll bar, and data if on outside of the editor.
    
    if( SRow <= CRow ) { ScrollBar.setValue( SRow ); }
      
    if( ERow >= ( CRow + ( TRows << 4 ) ) ) { ScrollBar.setValue( ERow - ( ( TRows - 1 ) << 4 ) ); }
    
    else { TModel.updateData(); }
  }
}
