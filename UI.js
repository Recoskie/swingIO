var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/"));

var treeNodes = ["f.gif","u.gif","H.gif","disk.gif","EXE.gif","dll.gif","sys.gif","ELF.gif","bmp.gif","jpg.gif","pal.gif","ani.gif","webp.gif","wav.gif","mid.gif","avi.gif"];

document.head.innerHTML += "<style>.vhex { position: relative; overflow-y: scroll; overflow-x: hidden; }\
.noSel { -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }\
.dataInspec { background:#CECECE; }.dataInspec table tr td { font-size:16px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width:50%; }\
.dataInspec table tr:nth-child(n+0):nth-child(-n+1) { background:#8E8E8E; }\
.dataInspec table tr:nth-child(n+2):nth-child(-n+17) { cursor: pointer; background:#FFFFFF; }\
.dataInspec fieldset { display: flex; justify-content: space-between; }\
#treeUL{ margin: 0; padding: 0; } #treeUL ul { list-style-type: none; } #treeUL div { white-space: nowrap; border: 0; }\
"+(function(nodes){for(var i = 0, o = ""; i < nodes.length; o+=".node"+i+"::before { content: url("+path+"/Icons/"+nodes[i++]+"); }");return(o);})(treeNodes)+"\
[class^='node']{ cursor: pointer; display:flex; align-items:center; width:0px; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }\
.nested { display: none; }.active { display: block; }\
.alert { background-color:#777777; padding: 20px; color: white; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);}\
.alertbg { width:100%; height:100%; background-color: rgba(0,0,0,0.8); position:absolute; top:0px; left:0px; }\
.closebtn{ margin-left:15px; color: white; font-weight:bold; float:right; font-size: 22px; line-height:20px; cursor:pointer; }.closebtn:hover{ color: black; }</style>";

/*------------------------------------------------------------
Optimized graphical text clipping.
--------------------------------------------------------------
In the future, we should have a next char width measure function to speed up such clipping operations rather than measuring the whole string.
In most cases, text fits the average and we never have to use average into remainder meaning this method is very fast and pixel-perfect across platforms.
------------------------------------------------------------*/

CanvasRenderingContext2D.prototype.drawString = function(text,x,y,width)
{
  var o = text.substring(0,width/this.avg&-1), i = o.length, b = null; width -= this.clipPrefix; for( var c = 0; c < 2; c++)
  {
    var re = 0; while( (re = width - this.measureText(o).width) > 0 && i < text.length )
    {
      re /= this.avg; re += i; while( i < re ) { o += text.charAt( i++ ); }
    }
    if( b == null ) { b = o.slice(0,-1)+"..."; width += this.clipPrefix; }
  }
  if( i < text.length || this.measureText(o).width > width ){ o = b; }; this.fillText(o,x,y);
}

//Calculating the average character for regular text and set font speeds up measurements by a lot.
//Should only be called once on setting the graphics context font.

CanvasRenderingContext2D.prototype.clipAvg = function()
{
  this.avg = 0; for( var i = 0x41; i < 0x5B; i++ ){ this.avg += this.measureText(String.fromCharCode(i)).width; this.avg += this.measureText(String.fromCharCode(i+0x20)).width; } this.avg /= 52;
  this.clipPrefix = this.measureText("...").width;
}
CanvasRenderingContext2D.prototype.avg = 7; CanvasRenderingContext2D.prototype.clipPrefix = 13;

/*------------------------------------------------------------
The main swingIO object is used to store references to other components and to share properties.
------------------------------------------------------------*/

swingIO = {
  //Component reference list.
  ref: [],
  //Scroll bar information.
  sBarWidth: null, sBarMax: null, sBarLowLim: null, sBarUpLim: null,
  /*------------------------------------------------------------
  Data types can be added or removed as you wish. Fully programable system.
  Data types are in pairs of 2 for little endian and big endian byte order.
  Blank felids are for data types that do not have a byte order.
  ------------------------------------------------------------*/
  dType: [ "Bit8",,
    "Int8",,
    "UInt8",,
    "Int16","LInt16",
    "UInt16","LUInt16",
    "Int32","LInt32",
    "UInt32","LUInt32",
    "Int64","LInt64",
    "UInt64","LUInt64",
    "Float32","LFloat32",
    "Float64","LFloat64",
    "Char8",,
    "Char16","LChar16",
    "String8",,
    "String16","LString16",
    "Other",,
    "Array"
  ],
  /*------------------------------------------------------------
  Number of bytes each data type is.
  The negative values are for variable length data types, and Arrays that are not exactly one data type but are a combination.
  ------------------------------------------------------------*/
  dLen: [1,1,1,2,2,4,4,8,8,4,8,1,2,-1,-1,-1,-2],
  /*------------------------------------------------------------
  Get scroll bar information via a component. Must be swingIO component format.
  Note that comp must contain the parent element and size must contain the canvas, or element to display in the component.
  ------------------------------------------------------------*/
  getScrollBarInfo: function( el )
  {
    if(!this.setSize) { return; } this.setSize(el,562949953421312); var o = el.size.clientHeight, n = 0;
    
    //Firefox sets height 0 when too large. In this case we must calculate max height.

    if( o == 0 )
    {
      this.setSize(el,n = 1); while( el.size.clientHeight != 0 ){ this.setSize(el,n <<= 1); } o = n >>= 1; n >>= 1; while( n > 0 ){ this.setSize(el,o | n); if( el.size.clientHeight != 0 ) { o |= n; } n >>= 1; }
    }
    
    this.sBarMax = o / 2;
    this.sBarWidth = el.comp.offsetWidth - el.comp.clientWidth;
    this.sBarLowLim = Math.floor(this.sBarMax * 0.05);
    this.sBarUpLim = this.sBarMax - this.sBarLowLim;

    o = n = this.setSize = undefined;
  },
  setSize: function(el,size) { el.size.style = "height:" + size + "px;min-height:" + size + "px;border:0;"; },
  /*------------------------------------------------------------
  Event handling.
  ------------------------------------------------------------*/
  scroll:function(r){this.ref[r].sc();},click:function(r){this.ref[r].select(window.event);},
  //Once dos font is used and loaded by a hex editor then the font reference object is no longer needed.
  dosFont: new FontFace('dos', 'url('+path+'/Font/DOS.ttf)')
}; treeNodes = path = undefined;

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

VHex.prototype.minDims = [0,0];

//Relative position parameters based on file size while scrolling data larger than the max scroll bar size.

VHex.prototype.rel = false, VHex.prototype.relPos = 0, VHex.prototype.relSize = 0, VHex.prototype.oldOff = 0, VHex.prototype.relDataUp = 0;

//The hex editor columns.

VHex.prototype.hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

function VHex( el, io, v )
{
  this.io = io; this.comp = document.getElementById(el); var e = "='swingIO.click("+swingIO.ref.length+");'";
  var w = this.comp.getAttribute("width") || this.comp.style.width || "0px;";
  var h = this.comp.getAttribute("height") || this.comp.style.height || "0px;";
  this.comp.outerHTML = "<div id='"+el+"' class='vhex noSel' onscroll='swingIO.scroll("+swingIO.ref.length+");' onpointerdown"+e+">\
  <canvas id='"+el+"g' style='position:sticky;top:0px;left:0px;background:#CECECE;z-index:-1;'></canvas><div id='"+el+"s'></div></div>"; e = undefined;
  
  this.comp = document.getElementById(el); this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d");
  
  //Visible on creation.
  
  this.hide( false );

  //Selected byte positions.

  this.sel = 0; this.sele = 0; this.slen = -1;
  
  //Find the width of the system scroll bar, and max height of scroll bar.
  //Find the lower and upper limit while scrolling data larger than scroll bar clip area is 5%.

  if( swingIO.sBarWidth == null ) { swingIO.getScrollBarInfo(this); }

  //Virtual or offset scroll.

  if( v )
  {
    this.relSize = 562949953421312; this.relDataUp = this.relSize - swingIO.sBarLowLim; this.rel = true;
    this.adjSize = function() { var s = swingIO.sBarMax - this.getRows(); this.size.style = "height:" + s + "px;min-height:" + s + "px;border:0;"; }
    this.setRows = function(){}; this.sc = this.virtualSc;
  }
  else { this.sc = this.offsetSc; }
  
  //Component min size.
  
  this.minDims = [682 + swingIO.sBarWidth, 256]; this.resetDims();
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)"; this.addcol = v ? -1 : 42;

  //Load Font.
  
  if( swingIO.dosFont ) { swingIO.dosFont.load().then(function(font){ document.fonts.add(font); swingIO.dosFont = undefined; }); } this.setRows(io.file.size);
  
  //Allows us to referenced the proper component to update on scroll.
  
  swingIO.ref[swingIO.ref.length] = this;
  
  //Add the component to the IO Event handler.
  
  io.comps[io.comps.length] = this; this.comp.style.width = w; this.comp.style.height = h; w = h = undefined;
}

//Scrolling event.

VHex.prototype.offsetSc = function(r)
{
  if(!r) { this.io.wait(this,"offsetSc"); return; }
  
  if( this.rel ){ this.adjRelPos(); }
  
  this.io.bufRead( this, "update" );

  this.io.seek(this.getPos() * 16);
  
  this.io.read(this.getRows() * 16);
}

VHex.prototype.virtualSc = function(r)
{
  if(!r) { this.io.wait(this,"virtualSc"); return; }
  
  this.adjRelPos();
  
  this.io.bufRead( this, "update" );

  this.io.seekV(this.getPos() * 16);
  
  this.io.readV(this.getRows() * 16);
}

//Blocks the scroll event when scroll bar is being adjusted.

VHex.prototype.blockSc = function() { if(this.virtual) { this.sc = this.virtualSc; } else { this.sc = this.offsetSc; } }

//Byte selection event.

VHex.prototype.select = function(e)
{
  var x = ((e.pageX || e.touches[0].pageX) - this.comp.offsetLeft) - 164, y = ((e.pageY || e.touches[0].pageY) - this.comp.offsetTop) - 16;

  if( x > 0 && y > 0 )
  {
    var pos = (this.virtual ? this.io.dataV.offset : this.io.data.offset);

    if( x < 355 ) { x = ( x / 22 ) & -1; } else if( this.text && x < 510 ) { x = ( ( x - 365 ) / 9 ) & -1; } else { return; }
    
    y = ( y / 16 ) & -1; pos += y * 16 + x;

    if( !this.virtual ) { this.io.seek( pos ); } else { this.io.seekV( pos ); }
  }
}

VHex.prototype.update = function(temp)
{
  var g = this.g, height = this.c.height = this.comp.clientHeight; this.c.width = this.comp.clientWidth;
  
  var data = (temp == 1) ? this.io.tempD : (!this.virtual ? this.io.data : this.io.dataV), pos = data.offset;
  
  g.font = "16px dos"; g.fillStyle = "#FFFFFF";
  
  g.fillRect(164, 16, this.end, height);
  
  g.stroke();

  if( this.sel >= 0 && this.sele >= 0 ) { this.selection(g, pos); }
  
  g.fillStyle = "#000000";
  
  g.fillText(this.s, this.addcol, 14);
  
  //Columns lines.
  
  for( var x = 166, i = 0; i < 16; x += 22, i++ )
  {
    g.fillText(this.hexCols[i], x, 14);
    
    g.moveTo(x+19, 16);

    g.lineTo(x+19, height);
  }

  //text output column.
  
  if( this.text ) { g.fillText("Text", 584, 14); }
  
  //Rows.
  
  for( var y = 16, i1 = 0, text = ""; y < height; y += 16, i1 += 16 )
  {
    for( var x = 166, i2 = 0, val = 0; i2 < 16; x += 22, i2++ )
    {
      val = data[i1+i2]; g.fillText(!isNaN(val) ? val.byte() : "??", x, y+13);
      
      if( this.text )
      { 
        val = !isNaN(val) ? val : 0x3F; if( val == 0 || val == 10 || val == 173 ) { val = 0x20; }

        text += String.fromCharCode( val );
      }
    }
    
    if( this.text ) { g.fillText( text, 528, y+13); text = ""; }
    
    g.moveTo(164, y); g.lineTo(514, y);
  }
  
  //Address and offset column.
  
  g.fillRect(0, 16, 164, height);
  
  g.stroke(); g.fillStyle = "#FFFFFF";
  
  height -= 16; for( var i = 0; i < height; i += 16 )
  {
    g.fillText((pos + i).address(), 0, i+29);
  }
  
  g.stroke();
  
  //Because of asynchronous reading which gives the best performance it is possible
  //that the hex editor position and buffer do not match. If they do not match we must call sc agine.
  
  if( (this.getPos() * 16) !== this.io.data.offset ) { console.log("Out of sync"); }
}

//Draw selected area.

VHex.prototype.selection = function(g, pos)
{
  g.fillStyle = "#9EB0C1";

  //End and start position must be in order for the coordinates to be translated properly.

  if( this.sel > this.sele ) { var t = this.sel; this.sel = this.sele; this.sele = t; }

  //Converts offsets to real 2D coordinates.

  var r1 = this.sel & 0xF, r2 = (this.sele+1) & 0xF, y1 = this.sel - pos - r1 + 16, y2 = (this.sele+1) - pos - r2 + 32;
  
  if( r2 == 0 ){ y2 -= 16; r2 = 16; } var x1 = r1 * 22, x2 = r2 * 22, mLine = y2 - y1 > 16;

  //Optimized Multi line selection.

  if( y2 > 16 && y1 < this.comp.clientHeight )
  {
    if( y1 < 16 ) { y1 = 16; r1 = x1 = 0; } if( y2 > this.comp.clientHeight ) { y2 = this.comp.clientHeight; r2 = 0; x2 = 352; }
    
    g.moveTo( 164 + x1, y1 ); if( mLine ) { g.lineTo( 516, y1 ); } else { g.lineTo( 164 + x2, y1 ); }
    
    if( x2 == 0 ) { g.lineTo( 516, y2 ); } else { if( mLine ) { g.lineTo( 516, y2 - 16 ); g.lineTo( 164 + x2, y2 - 16 ); } g.lineTo( 164 + x2, y2 ); }
    
    if( mLine ) { g.lineTo( 164, y2 ); } else { g.lineTo( 164 + x1, y2 ); }
    
    if( x1 == 0 ) { g.lineTo( 164, y1 ); } else { if( mLine ) { g.lineTo( 164, y1 + 16 ); g.lineTo( 164 + x1, y1 + 16 ); } g.lineTo( 164 + x1, y1 ); }
    
    if( this.text )
    {
      x1 = r1 * 9, x2 = r2 * 9; g.moveTo( 528 + x1, y1 );
    
      if(mLine) { g.lineTo( 672, y1 ); } else { g.lineTo( 528 + x2, y1 ); }
    
      if( x2 == 0 ) { g.lineTo( 672, y2 ); } else { if(mLine) { g.lineTo( 672, y2 - 16 ); g.lineTo( 528 + x2, y2 - 16 ); } g.lineTo( 528 + x2, y2 ); }
    
      if(mLine) { g.lineTo( 528, y2 ); } else { g.lineTo( 528 + x1, y2 ); }
    
      if( x1 == 0 ) { g.lineTo( 528, y1 ); } else { if(mLine) { g.lineTo( 528, y1 + 16 ); g.lineTo( 528 + x1, y1 + 16 ); } g.lineTo( 528 + x1, y1 ); }
    }
    
    g.closePath(); g.fill(); g.beginPath();
  }
}

//Basic UI controls.

VHex.prototype.setText = function( v ) { this.minDims = [(v ? 682 : 516) + swingIO.sBarWidth, 256]; this.end = (this.text = v) ? 518 : 352; this.comp.style.minWidth = this.minDims[0] + "px"; this.comp.style.minHeight = this.minDims[1] + "px"; if( this.visible ) { this.update(this.io); } }

VHex.prototype.getRows = function() { return( Math.floor( this.comp.clientHeight / 16 ) ); }

//It is important that we subtract what is visible from the scroll area otherwise we will scroll past the end.

VHex.prototype.setRows = function( size )
{
  size = Math.floor( size ); size -= this.getRows();

  //Scroll bar can only go so high before it hit's it's limit.

  if( swingIO.sBarMax != null )
  {
    if( size > swingIO.sBarMax ) { this.rel = true; this.relSize = size; this.relDataUp = this.relSize - swingIO.sBarLowLim; size = swingIO.sBarMax; } else { this.rel = false; }
  }

  //Set size.

  this.size.style = "height:" + size + "px;min-height:" + size + "px;border:0;";
}

//We want to keep an few extra rows so the user can see the end of the file.

VHex.prototype.adjSize = function() { this.setRows( ( this.io.file.size / 16 ) + 3 ); }

//Get the real position including relative scrolling if active.

VHex.prototype.getPos = function() { return( Math.floor( Math.max(0, this.rel ? this.relPos : this.comp.scrollTop ) ) ); }

//Adjust relative positioned scrolling.

VHex.prototype.adjRelPos = function()
{
  var offset = this.comp.scrollTop;

  //Delta difference in scroll bar controls the relative position.

  var delta = offset - this.oldOff; this.relPos += delta;

  //The scroll bar must not pass the rel down position unless rel position is less than rel down data.

  if( offset <= swingIO.sBarLowLim && this.relPos >= swingIO.sBarLowLim ) { offset = swingIO.sBarLowLim; }
  else if( this.relPos <= swingIO.sBarLowLim ) { this.relPos = offset; }

  //The scroll bar must not pass the rel Up position unless rel position is grater than rel up data.

  if( offset >= swingIO.sBarUpLim && this.relPos <= this.relDataUp ) { offset = swingIO.sBarUpLim; }
  else if(this.relPos >= this.relDataUp ) { this.relPos = this.relDataUp + ( offset - swingIO.sBarUpLim ); }

  //The only time the scroll bar passes the Rel UP or down position is when all that remains is that size of data.

  this.sc = this.blockSc; this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

VHex.prototype.onread = function() { }

//Select the byte we have seeked to in the IO system. If the byte is outside the hex editor, then update the position.

VHex.prototype.onseek = function( f )
{
  if(!this.io.fileInit) { this.io.buf = ( this.comp.clientHeight >> 4 ) << 4; this.adjSize(); this.comp.scrollTo(0,0); this.sc(); return; }
  
  this.oldOff = this.relPos + this.comp.scrollTo, pos = this.virtual ? f.virtual : f.offset;

  this.sele = ( this.sel = pos ) + (this.slen > 0 ? this.slen - 1 : 0);
    
  if( this.rel ) { this.adjRelPos(); } this.update(f);
}

VHex.prototype.validate = function()
{
  //Do not update components that are not visible.

  if(!this.visible){ return; }
  
  //We must update the scroll bar any time height does not match.

  if( this.c.height != this.comp.clientHeight ){ this.adjSize(); }

  //Does not match the memory buffer, then we must reload data and redraw the output.

  this.io.buf = ( this.comp.clientHeight >> 4 ) << 4;

  if( ((this.getPos() << 4) != (this.virtual ? this.io.dataV.offset : this.io.data.offset) || (((( this.comp.clientHeight >> 4 ) << 4 ) > (this.virtual ? this.io.dataV.length : this.io.data.length)))) || !this.io.fileInit ) { this.initData(false); }

  //Aligns in memory buffer but needs to draw more rows.

  else if( this.c.height>>4 < this.comp.clientHeight>>4 ) { this.update(); }
  
  //Expand the width of the canvas without redrawing everything.
  
  else if(this.c.width < this.comp.clientWidth)
  {
    var t = this.g.getImageData(0,0,this.c.width,this.c.height);
    this.c.width = this.comp.clientWidth; this.g.putImageData(t,0,0); t = undefined;
  }
}

//Always wait till io stream is available.

VHex.prototype.initData = function(r)
{
  r = r || false; if( !r ) { this.io.wait(this,"initData"); return; }
  
  //Now we are able to update the editors data.
  
  this.io.bufRead(this, "update");
    
  if(this.virtual)
  {
    this.io.seekV(0); this.io.readV(this.io.buf);
  }
  else
  {
    this.io.seek(0); this.io.read(this.io.buf);
  }
}

/*------------------------------------------------------------
This is a web based version of the Data type inspector originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/dataInspector.java
Note the data type list and byte data dLen should be shared between swingIO as both the dataDescriptor, and dataInspector share the data type indexes.
------------------------------------------------------------*/

dataInspector.prototype.minDims = null, dataInspector.prototype.minChar = null;

function dataInspector(el, io)
{
  this.io = io; var d = this.comp = document.getElementById(el); this.editors = [];

  //Text minium char width only needs to be calculated once. This is used to know the general length of an string before it is text-overflow: ellipsis.
  //This is so we do not convert more bytes into a String of text than we have to as the rest will not be visible.
  
  if( dataInspector.prototype.minChar == null )
  {
    //We create a reference to 2D graphics, but do not care about the non existing canvas.

    var g2d = document.createElement("canvas").getContext("2d");

    //Next we want the default font in the graphics context.
    
    g2d.font = window.getComputedStyle(this.comp, null).getPropertyValue('font');

    //We now calculate the minimum width of the character font.

    var min = g2d.measureText(" ").width; for( var i = 0x41, m = 0; i < 0x5B; i++ )
    {
      m = g2d.measureText(String.fromCharCode(i)).width; min = min > m ? m : min;
      m = g2d.measureText(String.fromCharCode(i+0x20)).width; min = min > m ? m : min;
    }
    
    dataInspector.prototype.minChar = min; g2d = undefined;
  }

  //Create the component.
  
  d.className = "dataInspec noSel";
  var out = "<table style='table-layout:fixed;width:0px;height:0px;'><tr><td>Data Type</td><td>Value</td></tr>", event = "='swingIO.ref["+swingIO.ref.length+"].setType(0);'";
  out += "<tr onpointerdown"+event+"><td>Binary (8 bit)</td><td>?</td></tr>";
  this.out = []; for(var i = 1; swingIO.dLen[i+1] > -2; i++) { event = "='swingIO.ref["+swingIO.ref.length+"].setType("+i+");'"; out += "<tr onpointerdown"+event+"><td>" + swingIO.dType[i<<1] + "</td><td>?</td></tr>"; }
  event = "='swingIO.ref["+swingIO.ref.length+"].setType("+i+");'"; out += "<tr onpointerdown"+event+"><td>Use No Data type</td><td>?</td></tr>";
  event = "onclick='swingIO.ref["+swingIO.ref.length+"].onseek(swingIO.ref["+swingIO.ref.length+"].io);'";
  out += "<tr><td colspan='2'><fieldset><legend>Byte Order</legend><span><input type='radio' "+event+" name='"+el+"o' value='0' checked='checked' />Little Endian</span><span style='width:50%;'><input type='radio' "+event+" name='"+el+"o' value='1' />Big Endian</span></fieldset></td><tr>";
  event = "onclick='swingIO.ref["+swingIO.ref.length+"].base = this.value;swingIO.ref["+swingIO.ref.length+"].onseek(swingIO.ref["+swingIO.ref.length+"].io);'";
  out += "<tr><td colspan='2'><fieldset><legend>Integer Base</legend><span><input type='radio' "+event+" name='"+el+"b' value='2' />Native Binary</span><span><input type='radio' "+event+" name='"+el+"b' value='8' />Octal</span><span><input type='radio' "+event+" name='"+el+"b' value='10' checked='checked' />Decimal</span><span><input type='radio' "+event+" name='"+el+"b' value='16' />Hexadecimal</span></fieldset></fieldset></td><tr>";
  out += "<tr><td colspan='2'><fieldset><legend>String Char Length</legend><input type='number' min='0' max='65536' step='1' style='width:100%;' onchange='swingIO.ref["+swingIO.ref.length+"].strLen = Math.min(this.value, 65536);swingIO.ref["+swingIO.ref.length+"].onseek(swingIO.ref["+swingIO.ref.length+"].io);' value='0' /></fieldset></td><tr>";
  
  d.innerHTML = out;
  
  //Byte order control.
  
  this.order = ([].slice.call(document.getElementsByName(el+"o"), 0)).reverse();
  
  //Setup data type outputs.
  
  this.td = d.getElementsByTagName("table")[0]; for(var i = 1; swingIO.dLen[this.out.length] > -2; i++) { this.out[this.out.length] = this.td.rows[i].cells[1]; }

  //User input string length is updated when clicking on a string data type as output element 16.

  this.input = this.td.rows[i+4].cells[0].getElementsByTagName("input")[0];

  //Set default number base and string length.
  
  this.base = 10; this.strLen = 0;

  //Set other type.
  
  this.out[this.out.length-1].innerHTML = ""; this.setType(this.out.length-1, 0);
  
  //Visible on creation.
  
  this.hide( false );
  
  //Component min size.
  
  var t = d.getElementsByTagName("table")[0];
  
  if(this.minDims == null) { dataInspector.prototype.minDims = [d.getElementsByTagName("fieldset")[1].clientWidth+16, t.clientHeight+32]; }
  
  t.style.minWidth=d.style.minWidth=this.minDims[0]; d.style.minHeight=this.minDims[1]; t.style.width = "100%"; t.style.height = "100%"; t = undefined;
  
  //Allows us to referenced the proper component on update.
  
  swingIO.ref[swingIO.ref.length] = this;
  
  //Add the component to the IO Event handler.
  
  io.comps[io.comps.length] = this;
}

dataInspector.prototype.setType = function(t, order, len)
{
  t = t >= (this.out.length-1) ? (this.out.length-1) : t; len = len || swingIO.dLen[t]; if(order != null) { this.order[order&-1].checked = true; }
  
  if(this.sel) { this.td.rows[this.sel].style.background = "#FFFFFF"; } this.td.rows[this.sel=t+1].style.background = "#9EB0C1";

  //Variable length string.

  if( t == (Descriptor.String8 >> 1) || t == (Descriptor.String16 >> 1) )
  {
    if( len < 0 ) { len = t == (Descriptor.String16 >> 1) ? this.strLen << 1 : this.strLen; }
    this.input.value = this.strLen = t == (Descriptor.String16 >> 1) ? len >> 1 : len;
  }
  
  //Update hex editor data length.
  
  for( var i = 0; i < this.editors.length; i++ ) { this.editors[i].slen = len; if(this.editors[i].visible) { this.editors[i].onseek(this.io); } }

  this.onseek(this.io);
}

dataInspector.prototype.onread = function( f ) { }

//Update data type outputs at new offset in data.

dataInspector.prototype.onseek = function( f )
{
  var v8 = 0, v16 = 0, v32 = 0, v64 = 0, float = 0, sing = 0, exp = 0, mantissa = 0;
  
  //Calculate data relative position in buffer.
  
  var rel = f.offset - f.data.offset;
    
  //Little endian, and big endian byte order.
    
  if( this.order[1].checked )
  {
    v32 |= v8 |= f.data[rel]; v32 |= (v16 |= f.data[rel+1]) << 8; v32 |= (f.data[rel+2] || 0x00) << 16; v32 += (f.data[rel+3] || 0x00) * 16777216;
    v64 |= f.data[rel+4]; v64 |= f.data[rel+5] << 8; v64 |= f.data[rel+6] << 16; v64 += (f.data[rel+7] || 0x00) * 16777216;
    v16 = (v16 << 8) | v8;
  }
  else
  {
    v64 |= f.data[rel+7]; v64 |= f.data[rel+6] << 8; v64 |= f.data[rel+5] << 16; v64 += (f.data[rel+4] || 0x00) * 16777216;
    v32 |= f.data[rel+3]; v32 |= f.data[rel+2] << 8; v32 |= (v16 |= f.data[rel+1]) << 16; v32 += (v8 |= f.data[rel]) * 16777216;
    v16 = (v8 << 8) | v16;
  }

  //Byte.

  this.out[0].innerHTML = v8.toStr(2).pad(8);

  //The integer types. Limit the number of base conversions when sing and unsigned match.
    
  this.out[1].innerHTML = (v8 >= 128 ? v8 - 256 : v8).toStr(this.base); this.out[2].innerHTML = v8 < 128 ? this.out[1].innerHTML : v8.toStr(this.base);
    this.out[3].innerHTML = (v16 >= 32768 ? v16 - 65536 : v16).toStr(this.base); this.out[4].innerHTML = v16 < 32768 ? this.out[3].innerHTML : v16.toStr(this.base);
  this.out[5].innerHTML = (v32&-1).toStr(this.base); this.out[6].innerHTML = v32 < 2147483648 ? this.out[5].innerHTML : v32.toStr(this.base);
    
  if( this.order[1].checked )
  {
    if( v64 >= 2147483648 )
    {
      this.out[7].innerHTML = "-" + (~v64+(v32 >= 2147483648 ? 0 : 1)).toString64(((~v32)+1),this.base);
      this.out[8].innerHTML = v64.toString64(v32,this.base);
    }
    else
    {
      this.out[7].innerHTML = this.out[8].innerHTML = v64.toString64(v32,this.base);
    }
  }
  else
  {
    if( v32 > 2147483648 )
    {
      this.out[7].innerHTML = "-" + (~v32+(v64 >= 2147483648 ? 0 : 1)).toString64(((~v64)+1),this.base);
      this.out[8].innerHTML = v32.toString64(v64,this.base);
    }
    else
    {
      this.out[7].innerHTML = this.out[8].innerHTML = v32.toString64(v64,this.base);
    }
  }
  
  //float32 number.
  
  sing = (v32 >> 31) & 1; exp = (v32 >> 23) & 0xFF; mantissa = (v32 & 0x7FFFFF);
  
  //Compute "0.Mantissa" to exponent.
  
  if(exp != 0xFF)
  {
    float = (((exp !== 0 ? 8388608 : 0 ) + mantissa) / 8388608) * Math.pow(2, exp - 0x7F);
  }
  else{ float = Infinity; }

  //Nan.

  if (!isFinite(float) && mantissa > 0) { float = NaN; }
  
  //Float32 range.
  
  float = float.toPrecision(9); float = float.indexOf("e",9) != -1 ? float : float * 1;
  
  //Float value with proper sing.
  
  this.out[9].innerHTML = sing >= 1 ? "-" + float : float;
  
  //float64 number.
  
  if( this.order[1].checked )
  {
    sing = (v64 >> 31) & 1; exp = (v64 >> 20) & 0x7FF; mantissa = ((v64 & 0xFFFFF) * 0x100000000) + v32;
  }
  else
  {
    sing = (v32 >> 31) & 1; exp = (v32 >> 20) & 0x7FF; mantissa = ((v32 & 0xFFFFF) * 0x100000000) + v64;
  }
  
  //Compute "0.Mantissa" to exponent.
  
  float = (((exp !== 0 ? 4503599627370496 : 0) + mantissa) / 4503599627370496) * Math.pow(2, exp - 0x3FF);

  //Nan.

  if (!isFinite(float) && mantissa > 0) { float = NaN; }

  //Float value with proper sing.
  
  this.out[10].innerHTML = sing >= 1 ? -float : float;
  
  //8bit char code.
  
  this.out[11].innerHTML = String.fromCharCode(v8);
  
  //16bit char code.
  
  this.out[12].innerHTML = String.fromCharCode(v16);

  //String 8 and 16. Char width, and length count.

  var maxCharLen = Math.min( this.strLen, this.out[13].clientWidth / this.minChar ), c = "";

  for( var i = 0, v = 0; i < maxCharLen; i++ ){ v = f.data[rel+i]; c += v == 0x3C ? "&lt;" : String.fromCharCode(v); } this.out[13].innerHTML = c; c = "";

  if( this.order[1].checked )
  {
    for( var i = 0, e = maxCharLen << 1, v = 0; i < e; i+=2 ) { v = (f.data[rel+i+1]<<8)+f.data[rel+i]; c += v == 0x3C ? "&lt;" : String.fromCharCode(v); }
  }
  else
  {
    for( var i = 0, e = maxCharLen << 1, v = 0; i < e; i+=2 ) { v = (f.data[rel+i]<<8)+f.data[rel+i+1]; c += v == 0x3C ? "&lt;" : String.fromCharCode(v); }
  }

  this.out[14].innerHTML = c; c = undefined;
}

dataInspector.prototype.addEditor = function( vhex ) { this.editors[this.editors.length] = vhex; }

/*------------------------------------------------------------
This is a web based version of the data model originally designed to run in Java.
And also https://github.com/Recoskie/swingIO/blob/Experimental/Descriptor.java
------------------------------------------------------------*/

//The position we wish to style binary data.

Descriptor.prototype.offset = 0;

//Singular data type.

function dataType(str,type) { this.des = str; this.type = type; this.ref = []; this.el = []; }

//Collection of data types in array order.

function arrayType(str,types)
{
  this.des = str; this.type = 32; this.ref = []; this.el = [];

  //Basic array properties.
  
  this.size = 0; this.len = 0; this.endRow = 0;

  //Arrays with more than one data type have a data type array element.
  
  this.dataTypes = types.length > 1 ? types.length + 1 : types.length;

  this.optimizeData(types);
}

//Construct the data descriptor.

function Descriptor(data)
{
  //This information is used to subtract row to find the individual items in array rel pos.

  this.arRows = [];

  //Number of rows that this descriptor will display.
  
  this.rows = 0;

  //Begin organizing data for optimized display.

  this.optimizeData(data);
    
  //Event handler for when data descriptor is set or user clicks on a property or value.
  
  this.Event = function(){}; this.event="Event"; this.source = this;
}

//Both the data descriptor and array store elements in relative positions and in optimized array indexes.
//Array differs in that each element has an size and a length for number of times the data types repeat.

arrayType.prototype.optimizeData = Descriptor.prototype.optimizeData = function( data )
{
  //Stores the data types that will be rendered.

  var isDes = this instanceof Descriptor, des = isDes ? this.des = [] : this.aDes = []; this.data = [];
  
  //This information is used to subtract row to find the individual items in rel pos.
  
  this.relPos = []; var length = 0; for( var i = 0, b = 0; i < data.length; i++ )
  {
    des[i] = data[i].des; this.data[i] = data[i].type; b = swingIO.dLen[data[i].type>>1];

    //Variable length data types must be able to reference the descriptor, or array.
    //This allows variable length data types to be adjusted.
      
    if( b == -1 ){ data[i].ref[data[data[i].el[data[i].el.length] = i].ref.length] = this; b = data[i].length(); }

    //Array data type.
    
    if( b == -2 )
    {
      data[i].ref[data[data[i].el[data[i].el.length] = i].ref.length] = this; b = data[i].size * data[i].length();

      //Only descriptors have the array rows.
      
      if( isDes ) { this.arRows[this.arRows.length] = i + 1; this.arRows[this.arRows.length] = data[i]; this.rows += data[i].endRow; }
    }

    this.relPos[i] = length; length += b; if( isDes ) { this.rows += 1; }
  }
    
  this.relPos[this.relPos.length] = length; if( !isDes ) { this.size = length; }
}

//Variable length data types modify the relative positions of the descriptors they are added to.

dataType.prototype.length = arrayType.prototype.length = function(size)
{
  var rDelta = 0, delta = 0, el = 0, r = [], arType = this instanceof arrayType;

  //Return length of array, or data type bytes.

  if(size == null) { if(arType) { return(this.len); } else { el = this.el[0]; r = this.ref[0].relPos; return((r[el+1] - r[el]) || 0); } }

  //Data types adjust number of bytes changing relative byte positions.
  //Arrays adjust size by length of each array element adjusting both rows and number of bytes.
  
  if( arType ) { size = this.size * (this.len = size); rDelta = this.endRow; rDelta = (this.endRow = this.len * this.dataTypes) - rDelta; }

  //Update relative byte positions plus number of rows for arrays.
  
  for(var i = 0; i < this.ref.length; i++)
  {
    el = this.el[i]; r = this.ref[i].relPos; delta = size - (r[el+1] - r[el]);

    //Skip deltas that are zero.
    
    if( delta != 0 )
    {
      this.ref[i].rows += rDelta; el+=1; for(; el < r.length; r[el++] += delta);

      //If adjustable data type is inside an array.

      if( this.ref[i].size ) { this.ref[i].size += delta; this.ref[i].length(this.ref[i].length()); }
    }
  }
}

//Get data relative position by row number.

Descriptor.prototype.rel = function(r)
{
  //Check if data is within the array data type.

  for( var i = 0; i < this.arRows.length; i += 2 )
  {
    var arRow = this.arRows[i], array = this.arRows[i+1];

    //The start and end rows of the array.

    if( r >= arRow && r < (arRow + array.endRow) )
    {
      r -= arRow; var arEl = (r / array.dataTypes) & -1, arType = r % array.dataTypes;

      //If Data types are larger than one and align with the first element then it is the array element row.

      if( array.dataTypes > 1 ) { if( arType == 0 ) { return( (arEl * array.size) + this.relPos[arRow-1] ); } arType -= 1; }

      //The array data type selector is -1 if we land on the array element row.
        
      if( arType >= 0 ) { return(((arEl * array.size) + this.relPos[arRow-1]) + array.relPos[arType]); }
    }

    //Row difference because of array.

    else if( r >= (arRow + array.endRow) ){ r -= array.endRow; }
  }
  
  return( this.relPos[r] );
}

//Sets the method that is called when user clicks a data type.

Descriptor.prototype.setEvent = function( s, e ) { this.event = e; this.source = s; }

//The total length of the data.

Descriptor.prototype.length = function() { return( this.relPos[this.relPos.length - 1] ); }

//Construct the Descriptor data type keys from the swingIO data types list.

for(var i=0;i<swingIO.dType.length;swingIO.dType[i]&&(Descriptor[swingIO.dType[i]]=i),i++);

/*------------------------------------------------------------
This is a web based version of the experimental optimized data model originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/Experimental/dataDescriptor.java
------------------------------------------------------------*/

dataDescriptor.prototype.di = null, dataDescriptor.prototype.data = new Descriptor([]);
dataDescriptor.prototype.minDims = null, dataDescriptor.prototype.textWidth = [], dataDescriptor.prototype.minHex = 28;

function dataDescriptor( el, io )
{
  this.io = io; this.comp = document.getElementById(el); var e = "='swingIO.click("+swingIO.ref.length+");'";
  var w = this.comp.getAttribute("width") || this.comp.style.width || "0px;";
  var h = this.comp.getAttribute("height") || this.comp.style.height || "0px;";
  document.getElementById(el).outerHTML = "<div id='"+el+"' class='vhex noSel' style='overflow-y:auto;' onscroll='swingIO.scroll("+swingIO.ref.length+");' onpointerdown"+e+">\
  <canvas id='"+el+"g' style='position:sticky;top:0px;left:0px;background:#FFFFFF;z-index:-1;'></canvas><div style='border: 0;' id='"+el+"s'></div></div>"; e = undefined;

  this.comp = document.getElementById(el); this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d"); this.hide(false);

  //We should only ever measure this once.

  this.g.font = "14px " + this.g.font.split(" ")[1]; if( this.textWidth[0] == null )
  {
    dataDescriptor.prototype.textWidth = [this.g.measureText("Use").width>>1,this.g.measureText("Raw Data").width>>1,this.g.measureText("Data Type").width>>1,
    this.g.measureText("Operation").width>>1, this.g.measureText("Address").width>>1];
  
    //Minium width of hex characters. We do not want to translate a lot more hex values that what will fit into a table cell.
    
    var w = Infinity; for( var i = 0; i < 16; w = Math.min(w, this.g.measureText((( i < 10 ) ? i : String.fromCharCode(55 + i)) + "").width), i++ );
    dataDescriptor.prototype.minHex = w * 2 + this.g.measureText(" ").width - 2; w = undefined;
  }

  //Get scrollbar information if not already loaded.

  if( swingIO.sBarWidth == null ) { swingIO.getScrollBarInfo(this); }

  //Component minimum size.

  if( this.minDims == null ){ dataDescriptor.prototype.minDims = [((this.textWidth[0]+this.textWidth[1]+this.textWidth[2])<<1) + swingIO.sBarWidth + 64, 192]; } this.resetDims();

  //Selected element.

  this.rel1 = 0; this.rel2 = 0; this.type = 0; this.selectedRow = -1;
  
  //Allows us to referenced the proper component to update on scroll.
  
  swingIO.ref[swingIO.ref.length] = this; this.comp.style.width = w; this.comp.style.height = h; w = h = undefined;
}

//Scrolling event.

dataDescriptor.prototype.sc = function() { this.update(); }

dataDescriptor.prototype.select = function(e)
{
  this.selectedRow = (this.comp.scrollTop + ( (((e.pageY || e.touches[0].pageY) - this.comp.offsetTop ) ) >> 4 ))&-1; if( this.selectedRow < 1 || this.data.rows == 0 ) { return; }
  this.selectedRow = Math.min( this.selectedRow, this.data.rows ) - 1;

  //Data type descriptor.

  var r = this.selectedRow; if( this.update == this.dataCheck )
  {
    //Check if data is within the array data type.

    this.rel1 = null; this.rel2 = null; for( var i = 0; i < this.data.arRows.length; i += 2 )
    {
      var arRow = this.data.arRows[i], array = this.data.arRows[i+1];

      //The start and end rows of the array.

      if( r >= arRow && r < (arRow + array.endRow) )
      {
        //Subtract the current row to start of array.
        //Divide the row by number of data type rows to find the array element number.
        //Do division remainder of data type rows to find the data type in array element.

        var arEl = ((r - arRow) / array.dataTypes) & -1, arType = (r - arRow) % array.dataTypes;

        //If Data types are larger than one and align with the first element then it is the array element row.

        if( array.dataTypes > 1 )
        {
          if( arType == 0 )
          {
            this.rel1 = (arEl * array.size) + this.data.relPos[arRow-1]; this.rel2 = this.rel1 + array.relPos[array.dataTypes-1]; this.type = 32;
          }
          arType -= 1;
        }

        //The array data type selector is -1 if we land on the array element row.
        
        if( arType >= 0 )
        {
          this.rel1 = (arEl * array.size) + this.data.relPos[arRow-1];

          this.rel2 = this.rel1 + array.relPos[arType + 1]; this.rel1 += array.relPos[arType];

          this.type = array.data[arType];
        }

        break;
      }

      //Row difference because of array.

      else if( r >= (arRow + array.endRow) ){ r -= array.endRow; }
    }

    //If relative position is null then the data is not in array.

    if( this.rel1 == null ) { this.rel1 = this.data.relPos[r]; this.rel2 = this.data.relPos[r+1]; this.type = this.data.data[r]; }

    //Data types should always be seeked and in buffer before setting data type unless it is zero in size.

    if(this.rel1 != this.rel2)
    {
      this.io.onSeek(this,"setDataType"); this.io.seek(this.data.offset + this.rel1);
    }

    //If the data is zero in size then we should display what the data felled is intended for.

    else { this.data.source[this.data.event](this.selectedRow); this.update(); }
  }

  //Processor core.

  else
  {
    if( r < ( this.data.linear.length >> 1 ) )
    {
      this.coreDisLoc(this.data.linear[r],false); this.update();
    }
    else if( ( r -= ( this.data.linear.length >> 1 ) ) < this.data.crawl.length )
    {
      this.coreDisLoc(this.data.crawl[r],true); this.update();
    }
    else
    {
      r -= this.data.crawl.length; r = r << 1; this.io.seekV( this.data.data_off[r] );
    }
  }
}

//Update is changeable based on if we are working with processor core data, or just file data.
//Before updating we must check if the buffer data is in the correct offset.

dataDescriptor.prototype.update = dataDescriptor.prototype.dataCheck = function(temp)
{
  this.minRows = Math.min( this.data.rows, ((this.comp.clientHeight / 16) + 0.5)&-1 );
  this.curRow = Math.max(Math.min(this.comp.scrollTop,this.data.rows), 0) & -1, this.endRow = Math.min( this.curRow + this.minRows, this.data.rows ) & - 1;

  //Data within the current buffer area.

  var dPos = this.data.rel(this.curRow) + this.data.offset, dEnd = this.data.rel(this.endRow) + this.data.offset, data = (temp == 1) ? this.io.tempD : this.io.data;
 
  if(data.offset <= dPos && (data.offset+data.length) >= dEnd) { this.dataUpdate(data); }

  //Else we need to load the data we need before updating the component. This is least likely to happen.

  else { this.io.onRead( this, "dataCheck", 1 ); this.io.seek(dPos); this.io.read(dEnd - dPos); }
}

//Update output as data model.

dataDescriptor.prototype.dataUpdate = function(data)
{
  var g = this.g, width = this.c.width = this.comp.clientWidth, cols = width / 3, colsH = cols >> 1; this.c.height = this.comp.clientHeight, str = "";

  //The first row explains what each column is.

  g.fillStyle = "#CECECE"; g.fillRect(0,0,width,16); g.stroke(); this.g.font = "14px " + this.g.font.split(" ")[1];

  //The Number of rows that will fit on screen.
 
  g.fillStyle = "#FFFFFF"; g.fillRect( 0, 16, width, this.minRows << 4 ); g.stroke();
 
  //Draw the column lines.
 
  g.fillStyle = g.strokeStyle = "#000000";
  
  var rows = (this.endRow - this.curRow + 1) << 4;
 
  g.moveTo(0, 0); g.lineTo(0, rows); g.moveTo(cols, 0); g.lineTo( cols, rows ); g.moveTo(cols << 1, 0); g.lineTo( cols << 1, rows);
     
  g.moveTo(0, 16); g.lineTo(width, 16);
 
  //Column names.
 
  g.fillText("Use", colsH - this.textWidth[0], 13); colsH += cols; g.fillText("Raw Data", colsH - this.textWidth[1], 13); colsH += cols; g.fillText("Data Type", colsH - this.textWidth[2], 13);
 
  //Used when rendering the array data type.

  var array = null, curArray = 0, arRow = 0, row = this.curRow, des = "", dType = null;

  //Fill in the columns based on the current position of the scroll bar.
 
  for( var i = this.curRow, posY = 32; i < this.endRow; posY += 16, i++, row++ )
  {
    //Selected row.

    if( i == this.selectedRow ){ g.stroke(); g.fillStyle = "#9EB0C1"; g.fillRect(0, posY - 16, width, 16); g.stroke(); g.fillStyle = "#000000"; }

    //If we are within the array data type.

    while( curArray < this.data.arRows.length )
    {
      arRow = this.data.arRows[curArray]; array = this.data.arRows[curArray+1];

      //The start and end rows of the array.

      if( row >= arRow && row < (arRow + array.endRow) )
      {
        //Subtract the current row to start of array.
        //Divide the row by number of data type rows to find the array element number.
        //Do division remainder of data type rows to find the data type in array element.

        var arEl = ((row - arRow) / array.dataTypes) & -1, arType = (row - arRow) % array.dataTypes;

        //If Data types are larger than one and align with the first element then insert the array element row.

        if( array.dataTypes > 1 )
        {
          if( arType == 0 )
          {
            this.rel1 = (arEl * array.size) + this.data.relPos[arRow-1]; this.rel2 = this.rel1 + array.relPos[array.dataTypes-1]

            dType = swingIO.dType[32]; des = "Array element " + arEl + "";
          }
          arType -= 1;
        }

        //The array data type selector is -1 if we inserted the array element row.
        
        if( arType >= 0 )
        {
          this.rel1 = (arEl * array.size) + this.data.relPos[arRow-1];

          this.rel2 = this.rel1 + array.relPos[arType + 1]; this.rel1 += array.relPos[arType];

          //Array that has one element has the (El #) prefix added.

          dType = swingIO.dType[array.data[arType]]; des = array.aDes[arType] + (array.dataTypes == 1 ? "(El " + arEl + ")" : "");
        }

        break;
      }

      //Arrays that exists before current row add row difference so that data types in the descriptor continue after the array.

      else if ( row >= (arRow + array.endRow) ){ row -= array.endRow; curArray += 2 } else { break; }
    }

    //Regular data types are stored in relative position and by description and data type.

    if( dType == null ) { this.rel1 = this.data.relPos[row]; this.rel2 = this.data.relPos[row+1]; des = this.data.des[row]; dType = swingIO.dType[this.data.data[row]]; }

    //Data type description.
 
    g.drawString( des, 2, posY - 3, cols-4 );
 
    //Convert data to bytes.
 
    var pos = (this.rel1+this.data.offset)-data.offset, size = this.rel2-this.rel1; size = Math.min( cols / this.minHex, size);
      
    for(var i2 = 0; i2 < size; str+=(data[pos+i2]&-1).byte()+((i2<(size-1))?" ":""), i2++);

    g.drawString(str, cols + 2, posY - 3, cols-4); str = "";

    //Show the related data type.
 
    g.fillText( dType, ( cols << 1 ) + 2, posY - 3 ); g.moveTo(0, posY); g.lineTo(width, posY);

    //Set data type null. This will be used to determine if the data is in array or is in relative position of the data type array.

    dType = null;
  }

  g.stroke();
}

//Update output as core data model.

dataDescriptor.prototype.coreUpdate = function()
{
  var g = this.g, width = this.c.width = this.comp.clientWidth, cols = width >> 1, colsH = cols >> 1; this.c.height = this.comp.clientHeight, str = "";

  //The first row explains what each column is.

  g.fillStyle = "#CECECE"; g.fillRect(0,0,width,16); g.stroke(); this.g.font = "14px " + this.g.font.split(" ")[1];
  
  //The Number of rows that will fit on screen.
  
  var curRow = Math.max( this.comp.scrollTop & -1, 0 ), minRows = Math.min( this.data.rows - curRow, ((this.comp.clientHeight / 16) + 0.5)&-1 );
  
  g.fillStyle = "#FFFFFF"; g.fillRect( 0, 16, width, minRows << 4 ); g.stroke();
  
  //Draw the column lines.
  
  g.fillStyle = g.strokeStyle = "#000000"; g.moveTo(cols, 0); g.lineTo(cols, (minRows+1) << 4); g.moveTo(0, 16); g.lineTo(width, 16);
  
  //Column names.
  
  g.fillText("Operation", colsH - this.textWidth[3], 13); colsH += cols; g.fillText("Address", colsH - this.textWidth[4], 13);
  
  //The current start and end row in the data by scroll bar position
  
  var endRow = Math.min( curRow + minRows, this.data.rows );
  
  //Display the addresses and operations that can be carried out.
  
  for( var i = curRow, posY = 32; i < endRow; posY += 16, i++ )
  {
    if( i == this.selectedRow ){ g.stroke(); g.fillStyle = "#9EB0C1"; g.fillRect(0, posY - 16, width, 16); g.stroke(); g.fillStyle = "#000000"; }
  
    //Each operation is sorted into a list as the core engine reads the binary.
  
    var row = i; if( row < ( this.data.linear.length >> 1 ) )
    {
      g.fillText( "LDisassemble", 2, posY - 3 );
      g.fillText( this.data.linear[ row << 1 ].address(), cols + 2, posY - 3 );
    }
    else if( ( row -= ( this.data.linear.length >> 1 ) ) < this.data.crawl.length )
    {
      g.fillText( "Disassemble", 2, posY - 3 );
      g.fillText( this.data.crawl[ row ].address(), cols + 2, posY - 3 );
    }
    else
    {
      row -= this.data.crawl.length;
      g.fillText( "Data", 2, posY - 3 );
      g.fillText( this.data.data_off[ row << 1 ].address(), cols + 2, posY - 3 );
    }
  
    g.moveTo(0, posY); g.lineTo(width, posY);
  }

  g.stroke();
}

//A programmable method for the actions to take when disassembling a location of code.

dataDescriptor.prototype.coreDisLoc = function(virtual, crawl) { }

//Clear the data model.

dataDescriptor.prototype.clear = function()
{
  this.update = this.dataCheck; this.minRows = this.curRow = this.endRow = 0;
  
  this.data = new Descriptor([]); this.selectedRow = -1; this.adjSize(); this.dataUpdate();
}

//Set the data model.

dataDescriptor.prototype.setDescriptor = function( d )
{
  this.update = this.dataCheck; this.data = d; this.selectedRow = -1;
  
  this.io.onSeek(this,"load"); this.io.seek(d.offset);
}

dataDescriptor.prototype.load = function()
{
  this.data.source[this.data.event]( -1, this.io.offset - this.io.data.offset ); this.di.setType(15, 0, this.data.relPos[this.data.relPos.length-1]);
  
  this.adjSize(); this.comp.scrollTo(0,0); this.update();
}

dataDescriptor.prototype.setDataType = function()
{
  var order = (this.type & 1) == 1; this.type >>= 1;

  this.data.source[this.data.event]( this.selectedRow, this.io.offset - this.io.data.offset );
  
  this.di.setType(this.type, order, this.rel2 - this.rel1); this.update();
}

//Set core data model.

dataDescriptor.prototype.setCore = function( c )
{
  this.update = this.coreUpdate; this.data = c; this.selectedRow = -1;
  
  this.adjSize(); this.comp.scrollTo(0,0); this.update();
}

dataDescriptor.prototype.setInspector = function( dInspector ) { this.di = dInspector; }

//Adjust scroll bar size on size change.

dataDescriptor.prototype.adjSize = function()
{
  var size = ( this.data.rows - ((this.comp.clientHeight / 16) - 1) ) + 2;

  this.size.style = "height:" + size + "px;min-height:" + size + "px;border:0;";
}

dataDescriptor.prototype.validate = function()
{
  //Do not update components that are not visible.

  if(!this.visible){ return; }
  
  //We must update the scroll bar any time height does not match.

  if( this.c.height != this.comp.clientHeight ){ this.adjSize(); }

  if( this.update == this.dataCheck )
  {
    this.minRows = Math.min( this.data.rows, ((this.comp.clientHeight / 16) + 0.5)&-1 );
    this.curRow = Math.max(Math.min(this.comp.scrollTop,this.data.rows), 0) & -1, this.endRow = Math.min( this.curRow + this.minRows, this.data.rows ) & - 1;
    
    //Data within the current buffer area.
    
    var dPos = this.data.rel(this.curRow) + this.data.offset, dEnd = this.data.rel(this.endRow) + this.data.offset;
     
    if(this.io.data.offset <= dPos && (this.io.data.offset+this.io.data.length) >= dEnd) { this.dataUpdate(this.io.data); }
    
    //Else we need to load the data we need before updating the component. This is least likely to happen.

    else if( !this.io.wait(this.validate) ) { this.io.onRead(this, "dataUpdate",this.io.tempD); this.io.seek(dPos); this.io.read(dEnd - dPos); }
  }
  else { this.update(); }
}

/*------------------------------------------------------------
This is a web based version of the binary tree tool originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/tree/JDTree.java
------------------------------------------------------------*/

treeNode.prototype.fileType = [ ".h", ".disk",
  ".com", ".exe", ".dll", ".sys", ".drv", ".ocx", ".efi", ".mui",
  ".axf", ".bin", ".elf", ".o", ".prx", ".puff", ".ko", ".mod", ".so",
  ".bmp", ".dib",
  ".jpg", ".jpeg", ".jpe", ".jif", ".jfif", ".jfi",
  ".pal",
  ".ani",
  ".webp",
  ".wav", ".rmi",
  ".avi"
];
treeNode.prototype.node = [ 2, 3,
  4, 4, 5, 6, 6, 6, 6, 6,
  7, 7, 7, 7, 7, 7, 7, 7, 7,
  8, 8,
  9, 9, 9, 9, 9, 9,
  10,
  11,
  12,
  13, 14,
  15
];

tree.prototype.minDims = [160,96], tree.prototype.selectedNode = null;

function tree(el) { this.comp = document.getElementById(el); this.resetDims(); this.comp.style.overflow = "auto"; }

//Set the tree nodes.

tree.prototype.set = function(v) { this.comp.onpointerdown = function(e){ tree.prototype.treeClick(e);}; this.comp.className = "noSel"; this.comp.innerHTML = "<ul id=\"treeUL\">" + v + "</ul>"; }

//Navigate the tree nodes. Does the same thing as treeNode getNode except this navigates the HTML list structure directly.

tree.prototype.getNode = function(i)
{
  var r = this.comp.firstChild.children[i]; if(r.children.length >= 2) { r = r.firstChild; } r = r.firstChild;

  r.setArgs = this.setArgs; r.getArgs = this.getArgs; r.setNode = this.setNode; r.length = this.length; r.name = r.innerHTML;  r.getNode = function( i )
  {
    var r = this.parentElement.parentElement.children[1].children[i]; if(r.children.length >= 2) { r = r.firstChild; } r = r.firstChild;
    r.setArgs = this.setArgs; r.getArgs = this.getArgs; r.setNode = this.setNode; r.length = this.length; r.name = r.innerHTML; r.getNode = this.getNode; return(r);
  }

  return(r);
}

//Number of nested nodes under HTML tree node. Does the same thing as the treeNode length operation except this operates on the HTML structure directly.

tree.prototype.length = function(){ return( (this.parentElement.parentElement.querySelector(".nested") || {children:{length:0}}).children.length ); }

//Expands or hides nested nodes and gives the HTML node to users custom event handling code.

tree.prototype.event = function(){}; tree.prototype.treeClick = function(v)
{
  if(!v.tagName)
  {
    v = document.elementFromPoint((v.pageX || v.touches[0].pageX) - window.scrollX, (v.pageY || v.touches[0].pageY) - window.scrollY); if( v.tagName == "DIV" ){ v = v.parentElement; }

    if(window.getComputedStyle(v,"before").getPropertyValue("content") == "none") { return; }
  }
  
  var node = v.tagName == "SPAN"; v = v.querySelector("div"); v.self = this; v.setArgs = this.setArgs; v.getArgs = this.getArgs; v.setNode = this.setNode;

  //Set the selected node.

  if(this.selectedNode) { this.selectedNode.style.backgroundColor=""; } v.style.backgroundColor="#9EB0C1";
  
  //We can hide or show sub elements simulating collapse, or expand nodes.

  if( node )
  {
    var t = v.parentElement.parentElement.querySelector(".nested");

    //Only collapse an node if double clicked.
  
    if(t.className!="nested active" || v == this.selectedNode ) { t.classList.toggle("active"); }
  }
  
  this.event(this.selectedNode = v);
}

//Modify nodes. Note these methods are referenced from the HTML node elements only.

tree.prototype.setArgs = function( arg ) { this.setAttribute("args", arg + ""); }
tree.prototype.getArgs = function() { return( this.getAttribute("args").split(",")); }
tree.prototype.setNode = function( node )
{
  var el = document.createElement("template"); el.innerHTML = node + ""; el = el.content.firstChild;

  if( (t = this.parentElement.parentElement).innerHTML.startsWith("<span") > 0 ){ t.children[1].remove(); } //Remove any nodes that have nested nodes.

  this.parentElement.parentElement.replaceChild(el,this.parentElement); if(this.self) { this.self.selectedNode = el.querySelector("div"); }
}

//The tree nodes.

function treeNode(n,args,expand)
{
  var t = 0; for(var i = 0; i < this.fileType.length; i++)
  {
    if( n.substring(n.length-this.fileType[i].length, n.length).toLowerCase() == this.fileType[i] )
    {
      t = this.node[i]; n = n.substring(0, n.length-this.fileType[i].length);
    }
  }

  //The first node has no nested children nodes until we add more nodes.

  this.nodes = ["<li class=\"node"+((t==0)?1:t)+"\"><div args='"+((args!=null)?args:"")+"'>"+n+"</div></li>"]; this.name = n;

  //The first node is replaced by 'node' as soon as we add more nodes to the nodes array.
  
  this.node = "<li><span class=\"node"+t+"\"><div args='"+((args!=null)?args:"")+"'>"+n+"</div></span><ul class=\"nested"+(expand?" active":"")+"\">";
}

treeNode.prototype.add = function(n,args)
{
  n = (n instanceof treeNode) ? n : new treeNode(n,args,false); if(this.node) { this.nodes[0] = this.node; this.node = undefined; } this.nodes[this.nodes.length] = n; n.parentNode = this;
}

//Get a tree node.

treeNode.prototype.getNode = function(i){ return(this.nodes[i+1]); }

//Number of tree nodes we can iterate though with "getNode".

treeNode.prototype.length = function() { return( this.nodes.length - 1 ); }

//Combines the html together of all nodes. Adds </ul></li> at the end of nodes with nested elements.

treeNode.prototype.toString = function() { for( var o = "", i = 0; i < this.nodes.length; o += this.nodes[i++] + "" ); return( o + (this.node?"":"</ul></li>") ); }

//Shared UI controls.

VHex.prototype.resetDims = dataInspector.prototype.resetDims = tree.prototype.resetDims = dataDescriptor.prototype.resetDims = function() { this.comp.style.width = this.comp.style.height = "unset"; this.comp.style.minWidth = this.minDims[0] + "px"; this.comp.style.minHeight = this.minDims[1] + "px"; }
VHex.prototype.minWidth = dataInspector.prototype.minWidth = tree.prototype.minWidth = dataDescriptor.prototype.minWidth = function( v ) { return(this.comp.style.minWidth = v || this.comp.style.minWidth); }
VHex.prototype.minHeight = dataInspector.prototype.minHeight = tree.prototype.minHeight = dataDescriptor.prototype.minHeight = function( v ) { return(this.comp.style.minHeight = v || this.comp.style.minHeight); }
VHex.prototype.width = dataInspector.prototype.width = tree.prototype.width = dataDescriptor.prototype.width = function( v ) { return(this.comp.style.width = v || this.comp.style.width); }
VHex.prototype.height = dataInspector.prototype.height = tree.prototype.height = dataDescriptor.prototype.height = function( v ) { return(this.comp.style.height = v || this.comp.style.height); }
VHex.prototype.hide = dataInspector.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; if(this.visible){ this.onseek(this.io); } }
tree.prototype.hide = dataDescriptor.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; }

//The default alert box sucks.

function alert(msg) { document.children[0].insertAdjacentHTML('beforeend','<div class="alertbg"><div class="alert"><span class="closebtn" onclick="this.parentElement.parentElement.remove();">X</span><br />'+msg+'<br /><br/><center><input type="button" value="OK" onclick="this.parentElement.parentElement.parentElement.remove();" /></center></div></div>'); }

//64bit lossless base conversion.

Number.prototype.toString64 = function(v32,base)
{
  var o = "", f = (r32 = this == 0) ? v32 : this * 4294967296, sec = base**((Math.log(f) / Math.log(base)) & -1), r = 0;
  
  while( sec > 1 )
  {
    r=(f/sec)&-1; f=(f-(r*sec)); r=Math.abs(r); o += ( r < 10 ) ? r : String.fromCharCode(55 + r);
    
    if( !r32 && sec < 4503599627370496 ) { f += v32; r32 = true; } sec /= base;
  }
  
  f=Math.abs(f); return( o + (( f < 10 ) ? f : String.fromCharCode(55 + f)) );
}

//Zero pad left side of number.

String.prototype.pad = function(len) { for( var s = this; s.length < len; s = "0" + s ); return(s); }

//Address format offsets.

if( Number.prototype.address == null )
{
  Number.prototype.address = function()
  {
    for( var s = this.toStr(16); s.length < 16; s = "0" + s ); return("0x"+s);
  }
}

//Byte format

Number.prototype.byte = function() { return((s = this.toStr(16)).length < 2 ? s = "0" + s : s); }

//Always uppercase.

Number.prototype.toStr = function(b) { return(this.toString(b).toUpperCase()); }