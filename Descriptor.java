package swingIO;

public class Descriptor
{
  //Data type values given name for convince.
  //Note the first binary digit is used for weather the type is in little endian or big endian.

  public static final int Bit8 = 0;
  public static final int Int8 = 2;
  public static final int UInt8 = 4;
  public static final int Int16 = 6;
  public static final int LInt16 = 7;
  public static final int UInt16 = 8;
  public static final int LUInt16 = 9;
  public static final int Int32 = 10;
  public static final int LInt32 = 11;
  public static final int UInt32 = 12;
  public static final int LUInt32 = 13;
  public static final int Int64 = 14;
  public static final int LInt64 = 15;
  public static final int UInt64 = 16;
  public static final int LUInt64 = 17;
  public static final int Float32 = 18;
  public static final int LFloat32 = 19;
  public static final int Float64 = 20;
  public static final int LFloat64 = 21;
  public static final int Char8 = 22;
  public static final int Char16 = 24;
  public static final int LChar16 = 25;
  public static final int String8 = 26;
  public static final int String16 = 28;
  public static final int LString16 = 29;
  public static final int Other = 30;
  public static final int Array = 32;
  public static final int[] bytes = new int[]{1,1,2,2,4,4,8,8,4,8,1,2,-1,-1,-1,-2};

  //Stores the data types that will be rendered.

  public int[] data = new int[]{};
  public String[] des = new String[]{};

  //Relative position of individual properties by row.

  public int[] relPos = new int[]{};

  //rows array starts at. This information is used to subtract row to find the individual items in rel pos.

  public int[] arPos = new int[]{};

  //Number of rows that this descriptor will display. Note when I add in set methods this value will change if array sizes change.
  
  public int rows = 0;

  //Event handler for when data descriptor is set or user clicks on a property or value.

  java.util.function.IntConsumer Event;

  //The position we wish to style binary data.

  public long pos = 0;

  //Construct the data descriptor.

  public Descriptor(dataType[] d)
  {
    des = new String[d.length]; data = new int[d.length];

    //Number of bytes descriptor stylized and rows.

    java.util.ArrayList<Integer> rPos = new java.util.ArrayList<Integer>();
    java.util.ArrayList<Integer> array = new java.util.ArrayList<Integer>();

    boolean defArray = false; int arrayEl = 0, arraySize = 0, arrayLen = 0, length = 0;

    for( int i = 0, b = 0; i < data.length; i++ )
    {
      des[i] = d[i].des; data[i] = d[i].type; b = bytes[data[i]>>1];
      
      if( b == -1 ){ i += 1; data[i] = d[i].type; b = data[i]; } else if( defArray = ( b == -2 ) )
      {
        array.add(rows); data[i+1] = d[i+1].type; data[i+2] = d[i+2].type;

        rows += ( (arrayEl = data[i+1]) + 1 ) * (arraySize = data[i+2]); i += 2;
      }
      
      if( !defArray ) { rPos.add(length); length += b; rows += 1; }
      else
      {
        arrayLen += b; if(!(defArray = (arrayEl-- > 0))) { length += arrayLen * arraySize; }
      }
    }
    
    rPos.add(length);

    relPos = new int[rPos.size()]; for( int i = 0; i < relPos.length; relPos[i] = rPos.get(i++) ); rPos.clear();
    arPos = new int[array.size()]; for( int i = 0; i < arPos.length; arPos[i] = array.get(i++) ); array.clear();
  }

  public Descriptor(java.util.ArrayList<dataType> d) { this( d.toArray( new dataType[ d.size() ] ) ); }

  //Calc number of bytes that need to be read to display rows.
  //For now we will not involve array types.

  public int bytes(int r1, int r2) { return( relPos[r2] - relPos[r1] ); }

  //Sets the method that is called when user clicks a data type.

  public void setEvent( java.util.function.IntConsumer e ) { Event = e; }

  //stud Event that does nothing.

  public void stud(int el){}

  //The total length of the data.

  public int length() { return( relPos[relPos.length - 1] ); }
}