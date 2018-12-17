# VHex
Fast hex UI componet, for RandomAccessFileV.

[RandomAccessFileV](https://github.com/Recoskie/RandomAccessFileV) Is a powerful mapping tool for fragmented data. This component is planed to have both an simulated Virtual memory display mode and offset mode in file. For mapping and modifying binary applications.

```java
import javax.swing.*;
import java.awt.*;

public class Window
{
  //Main file system stream.

  RandomAccessFileV file;

  public Window()
  {
    //Instance and setup an basic JFrame.

    JFrame frame = new JFrame( "Hex editor Component." );
    frame.setLocationRelativeTo( null );
    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

    //Create the file system stream.

    try
    {
      file = new RandomAccessFileV( "Test_files\\DIFxAPI.dll", "rw" );
    }
    catch( java.io.IOException e )
    {
      e.printStackTrace( System.out );
    }
    
    //VHex editor component.
    
    VHex hex = new VHex( file );

    //Add component to window.
    
    frame.add( hex );

    //Pack the frame.
    
    frame.pack();

    //Set the frame visible.

    frame.setVisible( true );
  }

  public static void main( String[] args )
  {
    new Window();
  }
}
```

![Example](Example.bmp)
