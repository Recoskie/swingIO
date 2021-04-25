# swingIO

Swing IO is a new set of GUI components that update on any read, write, or seek done on the IO system.

The file system <a href="https://github.com/Recoskie/RandomAccessFileV">RandomAccessFileV</a> implements the event system.

The new IO system can also map file, or disk positions as virtual addresses, and back to actual positions on a disk, or file.

Which is usefully for mapping binary applications, and ,making changes to binary applications.

RandomAccessFileV documentation also shows how to design a GUI-IO component.

Also you can mix swing IO components with java components like text boxes, buttons, check boxes, and layout managers.

------------------------------------------------------------
VHex Component
------------------------------------------------------------

Swing IO hex editor documentation: <a href="https://github.com/Recoskie/VHex">VHex</a>.

The hex editor can show mapped virtual address space. It can also show the raw binary data of the file, or disk.

Any seek to new position in a file, or disk that happens anywhere using RandomAccessFileV.

Will cause the hex editor to jump to location, and highlight the byte as blue.

When a read IO operation is done. The bytes that are read are highlighted in green.

When a write IO operation is done. The bytes are highlight in red that changed.

Weather you do the change in the hex editor, or in another component.

The hex editor will show anything you do as the IO system commands are always tracked.

------------------------------------------------------------

Clicking on any spot in the hex editor does a seek to position in RandomAccessFileV.

Which causes the hex editor to receive the seek, and to highlight the byte as blue.

This also triggers the seek event to all other GUI-IO components.

------------------------------------------------------------

When making changes to binary data using the hex editor. Uses the IO write operation.

Which causes the hex editor to receive the write, and to highlight the bytes as red that you are changing.

This also lets other components update that style the binary data when you make a change.

This way no matter how you change things all GUI-IO tools will show what changed visually.

Weather it is a data algorithm you built, or change done by some GUI tool.

------------------------------------------------------------
Other GUI-IO Components
------------------------------------------------------------

There are many other data components such as a descriptor data model.

The data model is loaded using the data descriptor GUI tool.

IT stylizes binary data, and seeks to binary data position when clicked on a type of data.

Which highlights the data blue in the hex editor.

The data inspector GUI component shows what type of data it is, and can also let you edit data types.

------------------------------------------------------------

There also is a specialized data tree Called JDTree.

Which can be used for organizing sections of binary data, or can be used with the file chooser for choosing a file, or disk.

------------------------------------------------------------

Lastly there is also a new layout manager called JCellPane include with this package.

------------------------------------------------------------

All of these GUI tools are designed to make binary data, and file formats visual, and easy to work with directly.

Each of these components can be used for what ever purpose you wish in any application.

All of these components can be set to read only mode by Opening a file, or disk in read only mode.

If you do not wish for the user to modifying anything, and wish to use it for displaying data.

------------------------------------------------------------

Better documentation on all the swing IO data components will be added at a later date.
