package swingIO.tree;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

public class JDTree extends JTree implements MouseListener, JDEventListener
{
  //This tree is meant to be set to a target event handler. Multiple event targets are not allowed.
  
  protected JDEventListener Event = this;
  protected JDNode t;
  protected TreePath p;

  //Icon manager.

  private static ImageIcon FolderPic[] = new ImageIcon[]
  {
    new ImageIcon( FileIconManager.class.getResource( "Icons/f.gif" ) ), //Folder
    new ImageIcon( FileIconManager.class.getResource( "Icons/u.gif" ) )  //File
  };

  private static ImageIcon h_file = new ImageIcon( FileIconManager.class.getResource( "Icons/H.gif" ) );
  private static ImageIcon disk = new ImageIcon( FileIconManager.class.getResource( "Icons/disk.gif" ) );
  private static ImageIcon exe_file = new ImageIcon( FileIconManager.class.getResource( "Icons/EXE.gif" ) );
  private static ImageIcon dll_file = new ImageIcon( FileIconManager.class.getResource( "Icons/dll.gif" ) );
  private static ImageIcon sys_file = new ImageIcon( FileIconManager.class.getResource( "Icons/sys.gif" ) );
  private static ImageIcon elf_file = new ImageIcon( FileIconManager.class.getResource( "Icons/ELF.gif" ) );
  private static ImageIcon bmp_file = new ImageIcon( FileIconManager.class.getResource( "Icons/bmp.gif" ) );
  private static ImageIcon jpg_file = new ImageIcon( FileIconManager.class.getResource( "Icons/jpg.gif" ) );
  private static ImageIcon pal_file = new ImageIcon( FileIconManager.class.getResource( "Icons/pal.gif" ) );
  private static ImageIcon ani_file = new ImageIcon( FileIconManager.class.getResource( "Icons/ani.gif" ) );
  private static ImageIcon webp_file = new ImageIcon( FileIconManager.class.getResource( "Icons/webp.gif" ) );
  private static ImageIcon wav_file = new ImageIcon( FileIconManager.class.getResource( "Icons/wav.gif" ) );
  private static ImageIcon midi_file = new ImageIcon( FileIconManager.class.getResource( "Icons/mid.gif" ) );
  private static ImageIcon avi_file = new ImageIcon( FileIconManager.class.getResource( "Icons/avi.gif" ) );
    
  public static String FType[] = new String[]
  {
    ".h", ".disk",
    ".com", ".exe", ".dll", ".sys", ".drv", ".ocx", ".efi", ".mui",
    ".axf", ".bin", ".elf", ".o", ".prx", ".puff", ".ko", ".mod", ".so",
    ".bmp", ".dib",
    ".jpg", ".jpeg", ".jpe", ".jif", ".jfif", ".jfi",
    ".pal",
    ".ani",
    ".webp",
    ".wav", ".rmi",
    ".avi"
  };
    
  public static ImageIcon LoadedPic[] = new ImageIcon[]
  {
    h_file, disk,
    exe_file, exe_file, dll_file, sys_file, sys_file, sys_file, sys_file, sys_file,
    elf_file, elf_file, elf_file, elf_file, elf_file, elf_file, elf_file, elf_file, elf_file,
    bmp_file, bmp_file,
    jpg_file, jpg_file, jpg_file, jpg_file, jpg_file, jpg_file,
    pal_file,
    ani_file,
    webp_file,
    wav_file, midi_file,
    avi_file
  };
    
  public boolean singleClick = false;

  protected String getExtension(String f)
  {
    if( f.lastIndexOf(46) > 0 )
    {
      return( f.substring( f.lastIndexOf(46), f.length() ).toLowerCase() );
    }
    
    return("");
  }

  class FileIconManager extends DefaultTreeCellRenderer
  {
    //Draw pictures related to file format icon.
    
    public Component getTreeCellRendererComponent(JTree tree,Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus)
    {
      boolean check = false;
    
      super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus );
    
      if( leaf ) { check = SetImage( value + "" ); }
    
      if( !check&leaf ) { UnknownFileType( value + "" ); } else if( !leaf ) { FolderIcon( value + "" ); }

      return( this );
    }
    
    //int value from a loaded set of icons in an array
    
    protected boolean SetImage(String name)
    {
      String EX = getExtension( name );
          
      if( !EX.equals("") )
      {
        int n = GetExtensionNumber( EX );
            
        if( n != -1 )
        {
          setIcon( LoadedPic[n] );
          setText( FilterExtension( name ) );
          return( true );
        }
      }
          
      return(false);
    }
    
    protected void FolderIcon(String f)
    {
      if( singleClick )
      {
        if( !SetImage(f) )
        {
          setIcon( FolderPic[0] );
        }
      }
      else
      {
        setIcon( FolderPic[0] );
      }
    }
    
    protected void UnknownFileType(String f) { setIcon( FolderPic[1] ); }
        
    protected int GetExtensionNumber(String Ex)
    {
      for(int i = 0; i < FType.length; i++ )
      {
        if( FType[i].equals(Ex) )
        {
          return(i);
        }
      }
          
      return(-1);
    }
        
    protected String FilterExtension(String File)
    {
      return( File.substring( 0, File.lastIndexOf(46) ) );
    }
  }

  //Set the event listener.

  public void setEventListener( JDEventListener listener ) { Event = listener; }

  //Set the event listener.

  public void removeEventListener( JDEventListener listener ) { Event = this; }

  public void mouseExited(MouseEvent e) { }
  
  public void mouseEntered(MouseEvent e) { }
  
  public void mouseReleased(MouseEvent e) { }

  public void mouseClicked(MouseEvent e) { }
  
  //Handel tree single click.

  public void mousePressed(MouseEvent e)
  {
    if( singleClick || e.getClickCount() == 2 )
    {
      this.setSelectionPath(this.getClosestPathForLocation(e.getX(), e.getY()));

      t = (JDNode)this.getLastSelectedPathComponent();

      if( t != null ) { Event.open( new JDEvent(this, t.toString(), getExtension(t.toString()), t.getID(), t.getArgs() ) ); }

      if( singleClick ){ this.expandPath( this.getLeadSelectionPath() ); }
    }
  }

  public JDTree() { this.addMouseListener(this); this.setCellRenderer(new FileIconManager()); this.setRowHeight( 30 ); }

  public JDTree( JDNode n ) { super(n); this.addMouseListener(this); this.setCellRenderer(new FileIconManager()); }

  //manually fire the open command.

  public void fireOpenEvent( JDEvent e ) { Event.open( e ); }
  public void fireOpenEvent( JDNode e )
  {
    p = new TreePath( e.getPath() ); this.setSelectionPath( p );

    Event.open( new JDEvent(this, e.toString(), getExtension(e.toString()), e.getID(), e.getArgs() ) );

    if( singleClick ){ this.expandPath( p ); }
  }

  //Settable Event handler.

  public void open( JDEvent e ) { }
}