# swingIO

Swing IO is a new set of GUI components that update on any read, write, or seek done on the IO system.

The file system <a href="https://github.com/Recoskie/RandomAccessFileV">RandomAccessFileV</a> implements the event system.

The new IO system can also map virtual addresses to actual positions on a disk, or file.

------------------------------------------------------------

Swing IO hex editor documentation: <a href="https://github.com/Recoskie/VHex">VHex</a>.

The hex editor can show mapped virtual addresses space. It can also show the raw binary data of the file, or disk.

Any seek to new position in a file, or disk that happens anywhere. Will cause the hex editor to jump to location.

Clicking on any spot in the hex editor will cause a seek which triggers the IO Seek event to all IO GUI components.

Keeping everything in sync.

When a read operation is done. The bytes that are read are highlighted in green.

When a write is done. The bytes are highlight in red that changed.

When making changes to binary data using the hex editor, or any data editor. Triggers the IO event write to all other components.

Keeping everything in sync.

------------------------------------------------------------

There are many other components such as a descriptor data model.

The data model is loaded using the data descriptor GUI tool.

IT stylizes binary data, and seeks to binary data position when clicked on a type of data.

Which highlights the data in the hex editor.

The data inspector GUI component shows what type of data it is, and can also let you edit data types.

------------------------------------------------------------

There also is a specialized data tree Called JDTree.

Which can be used for organizing sections of binary data, or can be used with the file chooser for choosing a file, or disk.

------------------------------------------------------------

Lastly there is also a new layout manager for components called JCellPane.

------------------------------------------------------------

All of these GUI tools are designed to make binary data, and file formats visual, and easy to work with directly by IO system.

Each of these components can be used for what ever purpose you wish in any application.

All of these components can be set to read only mode by Opening a file, or disk in read only mode.

If you do not wish for the user to modifying anything, and wish to use it for displaying data.

------------------------------------------------------------

Better documentation on all the swing IO data components will be added at a later date.
