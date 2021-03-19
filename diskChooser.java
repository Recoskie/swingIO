package swingIO;

import swingIO.tree.*;
import java.io.*;

public class diskChooser
{
  protected JDTree jd;

  //Check system information.

  private static final String Sys = System.getProperty("os.name");
  private static final boolean windows = Sys.startsWith("Windows");
  private static final boolean linux = Sys.startsWith("Linux");
  private static final boolean mac = Sys.startsWith("Mac");
  //private static final boolean solaris = Sys.startsWith("SunOS");
  //private static final boolean iOS = Sys.startsWith("iOS");

  //get system Disks.
  
  public class getDisks
  {
    private boolean end = false , check = false;
    private int r = 0;
    private File f;
    private JDNode root;

    public int disks = 0;
    
    public getDisks( JDNode r ){ root = r; }
      
    public void checkDisk( String Root, String type, boolean Zero )
    {
      r = 0; end = false; while(!end)
      {
        try
        {
          f = new File (Root + ( r == 0 && Zero ? "" : r ) + ""); check = f.exists(); new RandomAccessFile( f, "r");
          root.add( new JDNode( type + r + ".disk", Root + ( r == 0 && Zero ? "" : r ), -2 ) );
          r += 1; disks += 1;
        }
        catch( Exception er )
        {
          if( check || er.getMessage().indexOf("Access is denied") > 0 )
          {
            root.add( new JDNode( type + r + ".disk", Root + ( r == 0 && Zero ? "" : r ), -2 ) );
            r += 1; disks += 1;
          }
          else
          {
            end = true;
          }
        }
      }
    }
  }

  //Initialize.

  public boolean diskChooser( JDTree t ) { jd = t; return( findDisks() ); }

  public diskChooser( ) { }

  public boolean setTree( JDTree t ) { jd = t; return( findDisks() ); }

  //Search system for disks.

  private boolean findDisks()
  {
    //Clear the current tree nodes.

    ((javax.swing.tree.DefaultTreeModel)jd.getModel()).setRoot( null ); JDNode root = new JDNode("Root");

    //Setup disk check utility.

    getDisks d = new getDisks( root );
      
    //Windows uses Physical drive. Needs admin permission.

    if( windows ) { d.checkDisk( "\\\\.\\PhysicalDrive", "Disk", false ); }

    //Linux. Needs admin permission.
      
    if( linux ) { d.checkDisk("/dev/sda", "Disk", true ); d.checkDisk("/dev/sdb", "Removable Disk", true ); }

    //Mac OS X. Needs admin permission.

    if( mac ) { d.checkDisk("/dev/disk", "Disk", false ); }

    //Update tree.
      
    if( d.disks != 0 ) { ((javax.swing.tree.DefaultTreeModel)jd.getModel()).setRoot( root ); } else { return(false); }

    return( true );
  }
}