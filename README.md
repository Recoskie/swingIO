# VHex

[RandomAccessFileV](https://github.com/Recoskie/RandomAccessFileV) Is a powerful mapping tool for fragmented data. This component is designed to have both a simulated Virtual memory space mode, and file offset mode. For mapping and modifying binary applications. Or the virtual map can also be used to map fragmented data in disk images when doing data recovery.

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
      file = new RandomAccessFileV( "Sample.bin", "rw" );
    }
    catch( java.io.IOException e )
    {
      e.printStackTrace( System.out );
    }

    //Map a offset to a virtual address.

    file.addV( 0, 0xFF, 0x771241, 0xFF ); //Map first 256 bytes at 0x771241.
    file.addV( 0x14D0, 0x33, 0x771241, 0x33 ); //Write over first 0x33 bytes from offset 0x14D0.
    
    //Instance and setup VHex editor component.
    
    VHex Virtual = new VHex( file, true );
    VHex Offset = new VHex( file, false );

    //Add the two hex editor components side by side.

    frame.setLayout( new GridLayout( 1, 2 ) );
    
    frame.add( Virtual );
    frame.add( Offset );

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

![ExampleCode](ExampleCode.bmp)
