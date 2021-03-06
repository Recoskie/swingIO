package swingIO.tree;
import java.util.*;

//Event constructor.

public class JDEvent extends EventObject
{
  private String path = "";
  private String ext = ""; //File extension.
  private String id = ""; //A ID name can be attached after file extension.
  private long[] n = new long[0]; //Numbers can be attached after ID name.

  public JDEvent( Object source, String Path, String ex, String ID, long[] nv )
  {
    super( source ); path = Path; id = ID; ext = ex; n = nv;
  }

  public JDEvent( Object source, String Path, String ex, String ID, long nv )
  {
    super( source ); path = Path; id = ID; ext = ex; n = new long[]{nv};
  }

  public JDEvent( Object source, String Path, long[] nv )
  {
    super( source ); path = Path; n = nv;
  }

  public JDEvent( Object source, String Path, long nv )
  {
    super( source ); path = Path; n = new long[]{nv};
  }

  public JDEvent( Object source, String Path )
  {
    super( source ); path = Path;
  }

  public String getPath(){ return( path ); }

  public String getExtension(){ return( ext ); }

  public String getID(){ return( id ); }

  public long[] getArgs(){ return( n ); }

  public long getArg( int el ){ return( n[el] ); }
}