import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;

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

  //Number of rows in draw space.

  int TRows = 0;

  //The table model.

  AddressModel TModel;

  //The currently selected rows and cols in table. Relative to scroll bar.

  long SRow = 0, SCol = 0;
  long ERow = 0, ECol = 0;

  //The main hex edior display.

  class AddressModel extends AbstractTableModel
  {
    private String[] Offset = new String[] { "Offset (h)", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F" };

    //Byte buffer betwean io stream.

    private byte[] data = new byte[0x4A0];

    //Divide into rows of 16 offsets.

    private int RowLen = 16;

    //If virtual mode.

    public AddressModel(boolean mode)
    {
      if (mode)
      {
        Offset[0] = "Virtual Address (h)";
      }
    }

    //Get number of columns.

    public int getColumnCount()
    {
      return (Offset.length);
    }

    //Get number of rows in Display area.

    public int getRowCount()
    {
      return (TRows);
    }

    //Get the column.

    public String getColumnName(int col)
    {
      return (Offset[col]);
    }

    //The address col and byte values.

    public Object getValueAt(int row, int col)
    {
      //First col is address.

      if (col == 0)
      {
        return ("0x" + String.format("%1$016X", CurPos + (row * RowLen)));
      }

      //Else byte to hex.

      else if (((row * RowLen) + (col - 1)) < data.length && (((row * RowLen) + (col - 1)) + CurPos) < End)
      {
        return (String.format("%1$02X", data[(row * RowLen) + (col - 1)]));
      }

      else
      {
        return ("??");
      }
    }

    //JTable uses this method to determine the default renderer/editor for each cell.

    public Class getColumnClass(int c)
    {
      return (getValueAt(0, c).getClass());
    }

    //First column is not editbale as it is the address.

    public boolean isCellEditable(int row, int col)
    {
      return (col >= 1);
    }

    //Seting values writes directly to the IO stream.

    public void setValueAt(Object value, int row, int col)
    {
      int b = Integer.parseInt((String) value, 16);

      data[(row * RowLen) + (col - 1)] = (byte) b;

      //Write the new byte value to stream.

      try
      {
        //If offset mode use offset seek, and write.

        if (!Virtual)
        {
          IOStream.seek((row * RowLen) + (col - 1) + CurPos);
          IOStream.write(b);
        }

        //If Virtual use Virtual map seek, and write.

        else
        {
          IOStream.seekV((row * RowLen) + (col - 1) + CurPos);
          IOStream.writeV(b);
        }
      }
      catch (java.io.IOException e1)
      {}

      //Update table.

      fireTableDataChanged(); //fireTableCellUpdated(row, col);
    }

    //Update table data.

    public void updateData()
    {
      //Read data at scroll position.

      try
      {
        //If offset mode use offset seek.

        if (!Virtual)
        {
          IOStream.seek(CurPos);
          IOStream.read(data);
        }

        //If Virtual use Virtual map seek.

        else
        {
          IOStream.seekV(CurPos);
          IOStream.readV(data);
        }
      }
      catch (java.io.IOException e1) {}

      fireTableDataChanged();
    }
  }

  //The preferred table column size.

  class AddressColumnModel extends DefaultTableColumnModel
  {
    public void addColumn(TableColumn c)
    {
      //Address column.

      if (super.getColumnCount() == 0)
      {
        c.setPreferredWidth(136);
      }

      //Byte value columns.

      else
      {
        c.setPreferredWidth(20);
      }

      //Add column.

      super.addColumn(c);
    }
  }

  //A simple cell editor for my hex editor.

  class CellHexEditor extends DefaultCellEditor
  {
    final JTextField textField; //The table text componet.

    int pos = 0; //Charicter position in cell.

    int Row = 0, Col = 0; //Curent cell.

    boolean CellMove = false; //Moving cells.

    class HexDocument extends PlainDocument
    {
      @Override
      public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException
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

    public void UpdatePos()
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

        tdata.editCellAt(Row, Col); tdata.getEditorComponent().requestFocus();
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

        tdata.editCellAt(Row, Col); tdata.getEditorComponent().requestFocus();
        pos = 0; CellMove = true; return;
      }

      //Select the curent hex digit user is editing.

      CellMove = false;
    }

    public CellHexEditor()
    {
      super(new JTextField());
      textField = (JTextField) getComponent();

      textField.addFocusListener(new FocusAdapter()
      {
        @Override
        public void focusGained(FocusEvent e)
        {
          if (!CellMove)
          {
            pos = Math.max(0, textField.getCaretPosition() - 1);
          }
          textField.select(pos, pos + 1);
        }
      });

      textField.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mousePressed(MouseEvent e)
        {
          pos = Math.max(0, textField.getCaretPosition() - 1);
          textField.select(pos, pos + 1);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
          pos = Math.max(0, textField.getCaretPosition() - 1);
          textField.select(pos, pos + 1);
        }

        @Override
        public void mouseClicked(MouseEvent e)
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

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
      final JTextField textField = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);

      Row = row; Col = column; return (textField);
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

  //Only recaulatue number of table rows on resize. Speeds up table redering.

  class CalcRows extends ComponentAdapter
  {
    public void componentResized(ComponentEvent e)
    {
      TRows = (tdata.getHeight() / tdata.getRowHeight()) + 1;
      ScrollBar.setVisibleAmount(TRows);
    }
  }

  //If no mode setting then assume offset mode.

  public VHex(RandomAccessFileV f) { this(f, false); }

  //Initialize the hex UI component. With file system stream.

  public VHex(RandomAccessFileV f, boolean mode)
  {
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
      @Override

      public void mousePressed(MouseEvent e)
      {
        SRow = RelPos + ScrollBar.getValue() + tdata.rowAtPoint(e.getPoint());
        SCol = tdata.columnAtPoint(e.getPoint());

        ERow = SRow; ECol = SCol;

        TModel.fireTableDataChanged();
      }
    });

    tdata.addMouseMotionListener(new MouseMotionAdapter()
    {
      @Override

      public void mouseDragged(MouseEvent e)
      {
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
        CurPos = (RelPos + ScrollBar.getValue()) * 16;

        //If relative scrolling.

        if (Rel)
        {
          if (ScrollBar.getValue() > 1879048191)
          {
            RelPos = Math.max(RelPos + (ScrollBar.getValue() - 1879048191), 0x7FFFFFFF00000000L);
            if (RelPos < 0x7FFFFFFF80000000L)
            {
              ScrollBar.setValue(1879048191);
            }
          }

          else if (ScrollBar.getValue() < 268435456)
          {
            RelPos = Math.max(RelPos - (268435456 - ScrollBar.getValue()), 0);
            if (RelPos > 0)
            {
              ScrollBar.setValue(268435456);
            }
          }
        }
        
        if (tdata.isEditing()) { tdata.getCellEditor().stopCellEditing(); }

        TModel.updateData();
      }
    }

    ScrollBar.addAdjustmentListener(new Scroll());

    //Custom table selection rendering.

    tdata.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
    {
      @Override

      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int column)
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
      }
    });

    //Add everything to main component.

    super.setLayout(new BorderLayout());
    super.add(tdata.getTableHeader(), BorderLayout.PAGE_START);
    super.add(tdata, BorderLayout.CENTER);
    super.add(ScrollBar, BorderLayout.EAST);

    TModel.updateData();
  }
}
