var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/")), Ref = [];

var dosFont = new FontFace('dos', 'url('+path+'/Font/DOS.ttf)'), treeNodes = ["f.gif","u.gif","H.gif","disk.gif","EXE.gif","dll.gif","sys.gif","ELF.gif","bmp.gif","jpg.gif","pal.gif","ani.gif","webp.gif","wav.gif","mid.gif","avi.gif"];

document.head.innerHTML += "<style>.vhex { position: relative; overflow-y: scroll; overflow-x: hidden; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }\
.dataInspec { background:#CECECE; }.dataInspec table tr td { font-size:16px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width:50%; }\
.dataInspec table tr:nth-child(n+0):nth-child(-n+1) { background:#8E8E8E; }\
.dataInspec table tr:nth-child(n+2):nth-child(-n+17) { cursor: pointer; background:#FFFFFF; }\
.dataInspec fieldset { display: flex; justify-content: space-between; }\
#treeUL{ margin: 0; padding: 0; } #treeUL ul { list-style-type: none; } #treeUL div { white-space: nowrap; border: 0; }\
"+(function(nodes){for(var i = 0, o = ""; i < nodes.length; o+=".node"+i+"::before { content: url("+path+"/Icons/"+nodes[i++]+"); }");return(o);})(treeNodes)+"\
[class^='node']{ cursor: pointer; display:flex; align-items:center; width:0px; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }\
.nested { display: none; }.active { display: block; }</style>"; treeNodes = path = undefined;

var touchScreen = ("ontouchstart" in window) || (navigator.maxTouchPoints > 0) || (navigator.msMaxTouchPoints > 0), eventStr = touchScreen ? "ontouchstart" : "onmousedown";

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

VHex.prototype.minDims = [0,0], VHex.prototype.sBarWidth = null, VHex.prototype.sBarMax = null, VHex.prototype.sBarLowLim = null, VHex.prototype.sBarUpLim = null;

//Relative position parameters based on file size while scrolling data larger than the max scroll bar size.

VHex.prototype.rel = false, VHex.prototype.relPos = 0, VHex.prototype.relSize = 0, VHex.prototype.oldOff = 0, VHex.prototype.relDataUp = 0;

//The hex editor columns.

VHex.prototype.hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

function VHex( el, io, v )
{
  this.io = io; var h = this.comp = document.getElementById(el); h.className="vhex";

  h.innerHTML = "<canvas id=\""+el+"g\" style='position:sticky;top:0px;left:0px;background:#CECECE;z-index:-1;'></canvas><div id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d");
  
  //Visible on creation.
  
  this.hide( false );

  //Selected byte positions.

  this.sel = 0; this.sele = 0; this.slen = -1;
  
  //Find the width of the system scroll bar, and max height of scroll bar.
  //Find the lower and upper limit while scrolling.

  if( this.sBarWidth == null )
  {
    this.setRows(562949953421312); VHex.prototype.sBarMax = this.size.clientHeight / 2;
    VHex.prototype.sBarWidth = this.comp.offsetWidth - this.comp.clientWidth;
    VHex.prototype.sBarLowLim = Math.floor(this.sBarMax * 0.05);
    VHex.prototype.sBarUpLim = this.sBarMax - this.sBarLowLim;
  }

  //Virtual or offset scroll.

  if( v )
  {
    this.relSize = 562949953421312; this.relDataUp = this.relSize - this.sBarLowLim; this.rel = true;
    this.adjSize = function() { var s = this.sBarMax - this.getRows(); this.size.style = "height:" + s + "px;min-height:" + s + "px;border:0;"; }
    this.setRows = function(){}; this.sc = this.virtualSc;
  }
  else { this.sc = this.offsetSc; }
  
  //Component min size.
  
  this.minDims = [682 + this.sBarWidth, 256]; this.resetDims();
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)"; this.addcol = v ? -1 : 42;

  //Scroll.
  
  eval("var t = function(){Ref["+Ref.length+"].sc();}"); h.onscroll=t;

  //Seek byte onclick Event
  
  eval("var t = function(e){Ref["+Ref.length+"].select(e);}");
  
  //If touch screen.
 
  if(touchScreen) { this.comp.ontouchstart = t; } else { this.comp.onmousedown = t; }

  //Load Font.
  
  dosFont.load().then(function(font){ document.fonts.add(font); }); this.setRows(io.file.size);
  
  //Allows us to referenced the proper component to update on scroll.
  
  Ref[Ref.length] = this;
  
  //Add the component to the IO Event handler.
  
  io.comps[io.comps.length] = this;
}

//Scrolling event.

VHex.prototype.offsetSc = function()
{
  if( this.rel ){ this.adjRelPos(); }
  
  this.io.call( this, "update" );

  this.io.seek(this.getPos() * 16);
  
  this.io.read(this.getRows() * 16);
}

VHex.prototype.virtualSc = function()
{
  this.adjRelPos();
  
  this.io.call( this, "update" );

  this.io.seekV(this.getPos() * 16);
  
  this.io.readV(this.getRows() * 16);
}

//Byte selection event.

VHex.prototype.select = function(e)
{
  this.comp.focus();
  
  var x = ((e.pageX || e.touches[0].pageX) - this.comp.offsetLeft) - 164, y = ((e.pageY || e.touches[0].pageY) - this.comp.offsetTop) - 16;

  if( x > 0 && y > 0 )
  {
    var pos = this.getPos() * 16;

    if( x < 355 ) { x = ( x / 22 ) & -1; } else if( this.text && x < 510 ) { x = ( ( x - 365 ) / 9 ) & -1; } else { return; }
    
    y = ( y / 16 ) & -1; pos += y * 16 + x;

    if( !this.virtual ) { this.io.seek( pos ); } else { this.io.seekV( pos ); }
  }
}

//Method that checks what has to be updated on the hex editor after resize.

VHex.prototype.validate = function()
{
  //If component is not visible then just return.

  if( !this.visible ) { return; }

  //We must update the scroll bar any time height does not match.

  if( this.c.height != this.comp.offsetHeight ){ this.adjSize(); }

  //If canvas height is smaller we must rerender the output.

  if( this.c.height < this.comp.offsetHeight ) { this.sc(); }

  //If only width is smaller then we can set the canvas width larger.

  else if( this.c.width < this.comp.offsetWidth ) { this.c.width = this.comp.offsetWidth }
}

VHex.prototype.update = function(d)
{
  var g = this.g, height = this.c.height = this.comp.offsetHeight; this.c.width = this.comp.offsetWidth;
  
  var data = !this.virtual ? d.data : d.dataV, pos = !this.virtual ? this.io.data.offset : this.io.dataV.offset;
  
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

  if( y2 > 16 && y1 < this.comp.offsetHeight )
  {
    if( y1 < 16 ) { y1 = 16; r1 = x1 = 0; } if( y2 > this.comp.offsetHeight ) { y2 = this.comp.offsetHeight; r2 = 0; x2 = 352; }
    
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

VHex.prototype.setText = function( v ) { this.minDims = [(v ? 682 : 516) + this.sBarWidth, 256]; this.end = (this.text = v) ? 518 : 352; this.resetDims(); }

VHex.prototype.getRows = function() { return( Math.floor( this.comp.offsetHeight / 16 ) ); }

//It is important that we subtract what is visible from the scroll area otherwise we will scroll past the end.

VHex.prototype.setRows = function( size )
{
  size = Math.floor( size ); size -= this.getRows();

  //Scroll bar can only go so high before it hit's it's limit.

  if( this.sBarMax != null )
  {
    if( size > this.sBarMax ) { this.rel = true; this.relSize = size; this.relDataUp = this.relSize - this.sBarLowLim; size = this.sBarMax; } else { this.rel = false; }
  }

  //Set size.

  this.size.style = "height:" + size + "px;min-height:" + size + "px;border:0;";
}

//We want to keep an extra rows so the user can see the end of the file.

VHex.prototype.adjSize = function() { this.setRows( ( this.io.file.size / 16 ) + 1 ); }

//Get the real position including relative scrolling if active.

VHex.prototype.getPos = function() { return( Math.max(0, this.rel ? this.relPos : Math.floor( this.comp.scrollTop ) ) ); }

//Adjust relative scrolling or set position directly.

VHex.prototype.setPos = function( offset )
{
  this.comp.scrollTo( 0, offset ); this.oldOff = this.comp.scrollTop;
  
  if( this.rel ){ this.relPos = offset; this.adjRelPos(); }
  
  this.sc();
}

//Adjust relative positioned scrolling.

VHex.prototype.adjRelPos = function()
{
  var offset = this.comp.scrollTop;

  //Delta difference in scroll bar controls the relative position.

  var delta = offset - this.oldOff; this.relPos += delta;

  //The scroll bar must not pass the rel down position unless rel position is less than rel down data.

  if( offset <= this.sBarLowLim && this.relPos >= this.sBarLowLim ) { offset = this.sBarLowLim; }
  else if( this.relPos <= this.sBarLowLim ) { this.relPos = offset; }

  //The scroll bar must not pass the rel Up position unless rel position is grater than rel up data.

  if( offset >= this.sBarUpLim && this.relPos <= this.relDataUp ) { offset = this.sBarUpLim; }
  else if(this.relPos >= this.relDataUp ) { this.relPos = this.relDataUp + ( offset - this.sBarUpLim ); }

  //The only time the scroll bar passes the Rel UP or down position is when all that remains is that size of data.

  this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

VHex.prototype.onread = function( f ) { }

//Select the byte we have seeked to in the IO system. If the byte is outside the hex editor, then update the position.

VHex.prototype.onseek = function( f )
{
  if( this.virtual && f.curVra.Mapped ) { this.sele = ( this.sel = f.virtual ) + (this.slen > 0 ? this.slen - 1 : 0); } else if( !this.virtual ) { this.sele = ( this.sel = f.offset ) + (this.slen > 0 ? this.slen - 1 : 0); }
  
  var ds = !this.virtual ? this.io.offset : this.io.virtual;
  
  var ve = 0, vs = 0;
  
  if( !this.virtual )
  {
    vs = this.io.data.offset;
    ve = vs + (this.getRows() * 16);
  }
  else
  {
    vs = this.io.dataV.offset;
    ve = vs + (this.getRows() * 16);
  }
  
  if( ds > ve || ds < vs )
  {
    this.setPos(Math.floor(ds/16));
  }
  else
  {
    this.update(this.io);
  }
}

/*------------------------------------------------------------
This is a web based version of the Data type inspector originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/dataInspector.java
------------------------------------------------------------*/

dataInspector.prototype.dType = ["Binary (8 bit)","Int8","UInt8","Int16","UInt16","Int32","UInt32","Int64","UInt64","Float32","Float64","Char8","Char16","String8","String16","Use No Data type"];
dataInspector.prototype.dLen = [1,1,1,2,2,4,4,8,8,4,8,1,2,0,0,-1], dataInspector.prototype.minDims = null;

function dataInspector(el, io)
{
  this.io = io; var d = this.comp = document.getElementById(el);
  this.editors = [];
  
  d.className = "dataInspec";
  
  var out = "<table style='table-layout:fixed;width:0px;height:0px;'><tr><td>Data Type</td><td>Value</td></tr>";
  
  this.out = [];
  
  for(var i = 0; i < this.dType.length; i++) { out += "<tr "+eventStr+"='Ref["+Ref.length+"].setType("+i+");'><td>" + this.dType[i] + "</td><td>?</td></tr>"; }
  
  var event = "onclick='Ref["+Ref.length+"].order = this.value;Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);'";
  
  out += "<tr><td colspan='2'><fieldset><legend>Byte Order</legend><span><input type='radio' "+event+" name='"+el+"o' value='0' checked='checked' />Little Endian</span><span style='width:50%;'><input type='radio' "+event+" name='"+el+"o' value='1' />Big Endian</span></fieldset></td><tr>";
  
  event = "onclick='Ref["+Ref.length+"].base = this.value;Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);'";
  
  out += "<tr><td colspan='2'><fieldset><legend>Integer Base</legend><span><input type='radio' "+event+" name='"+el+"b' value='2' />Native Binary</span><span><input type='radio' "+event+" name='"+el+"b' value='8' />Octal</span><span><input type='radio' "+event+" name='"+el+"b' value='10' checked='checked' />Decimal</span><span><input type='radio' "+event+" name='"+el+"b' value='16' />Hexadecimal</span></fieldset></fieldset></td><tr>";
  
  out += "<tr><td colspan='2'><fieldset><legend>String Char Length</legend><input type='number' min='0' max='65536' step='1' style='width:100%;' onchange='Ref["+Ref.length+"].dLen[14] = (Ref["+Ref.length+"].dLen[13] = this.value = Ref["+Ref.length+"].strLen = Math.min( this.value, 65536)) << 1;Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);' value='0' /></fieldset></td><tr>";
  
  d.innerHTML = out;
  
  //Setup data type outputs.
  
  this.td = d.getElementsByTagName("table")[0];
  
  for(var i = 1; i <= this.dType.length; i++) { this.out[this.out.length] = this.td.rows[i].cells[1]; }
  
  this.order = 0; this.base = 10; this.strLen = 0;
  
  this.out[15].innerHTML = ""; this.setType(15);
  
  //Visible on creation.
  
  this.hide( false );
  
  //Component min size.
  
  var t = d.getElementsByTagName("table")[0];
  
  if(this.minDims == null) { dataInspector.prototype.minDims = [d.getElementsByTagName("fieldset")[1].offsetWidth+16, t.offsetHeight+32]; }
  
  t.style.minWidth=d.style.minWidth=this.minDims[0]; d.style.minHeight=this.minDims[1];
  
  t.style.width = "100%"; t.style.height = "100%"; t = undefined;
  
  //Allows us to referenced the proper component on update.
  
  Ref[Ref.length] = this;
  
  //Add the component to the IO Event handler.
  
  io.comps[io.comps.length] = this;
}

dataInspector.prototype.setType = function(t)
{
  if(this.sel)
  {
    this.td.rows[this.sel].style.background = "#FFFFFF";
  }
  this.td.rows[this.sel=t+1].style.background = "#9EB0C1";
  
  //Update hex editor data length.
  
  for( var i = 0; i < this.editors.length; i++ )
  {
    this.editors[i].slen = this.dLen[t];
    if(this.editors[i].visible){this.editors[i].onseek(this.io);}
  }
}

dataInspector.prototype.onread = function( f ) { }

//Update data type outputs at new offset in data.

dataInspector.prototype.onseek = function( f )
{
  if(((rel = f.offset - f.data.offset)+7) < f.data.length)
  {
    var v8 = 0, v16 = 0, v32 = 0, v64 = 0, float = 0, sing = 0, exp = 0, mantissa = 0;
    
    //Little endian, and big endian byte order.
    
    if( this.order == 0 )
    {
      v32 = v8=f.data[rel]; v32 |= (v16=f.data[rel+1]) << 8; v32 |= f.data[rel+2] << 16; v32 += f.data[rel+3] * 16777216;
      v64 = f.data[rel+4]; v64 |= f.data[rel+5] << 8; v64 |= f.data[rel+6] << 16; v64 += f.data[rel+7] * 16777216;
      v16 = (v16 << 8) | v8;
    }
    else
    {
      v64 = f.data[rel+7]; v64 |= f.data[rel+6] << 8; v64 |= f.data[rel+5] << 16; v64 += f.data[rel+4] * 16777216;
      v32 = f.data[rel+3]; v32 |= f.data[rel+2] << 8; v32 |= (v16=f.data[rel+1]) << 16; v32 += (v8=f.data[rel]) * 16777216;
      v16 = (v8 << 8) | v16;
    }

    //Byte.

    this.out[0].innerHTML = v8.toString(2).pad(8);

    //The integer types. Limit the number of base conversions when sing and unsigned match.
    
    this.out[1].innerHTML = (v8 >= 128 ? v8 - 256 : v8).toString(this.base); this.out[2].innerHTML = v8 < 128 ? this.out[1].innerHTML : v8.toString(this.base);
    this.out[3].innerHTML = (v16 >= 32768 ? v16 - 65536 : v16).toString(this.base); this.out[4].innerHTML = v16 < 32768 ? this.out[3].innerHTML : v16.toString(this.base);
    this.out[5].innerHTML = (v32&-1).toString(this.base); this.out[6].innerHTML = v32 < 2147483648 ? this.out[5].innerHTML : v32.toString(this.base);
    
    if( this.order == 0 )
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
  
  if( this.order == 0 )
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

  this.out[13].innerHTML = this.out[14].innerHTML = "<span></span>";
  var width = this.out[0].offsetWidth, text = this.out[13].getElementsByTagName("span")[0];
  
  for( var i = 0, c = ""; i < this.strLen && text.offsetWidth < width; i++ ){ c = String.fromCharCode(f.data[rel+i]); text.innerHTML += c; }
  
  text = this.out[14].getElementsByTagName("span")[0];
  
  if(this.order == 0)
  {
    for( var i = 0, e = this.strLen << 1; i < e && text.offsetWidth < width; i+=2 ) { text.innerHTML += String.fromCharCode((f.data[rel+i+1]<<8)+f.data[rel+i]); }
  }
  else
  {
    for( var i = 0, e = this.strLen << 1; i < e && text.offsetWidth < width; i+=2 ) { text.innerHTML += String.fromCharCode((f.data[rel+i]<<8)+f.data[rel+i+1]); }
  }
}

dataInspector.prototype.addEditor = function( vhex ) { this.editors[this.editors.length] = vhex; }

/*------------------------------------------------------------
This is a web based version of the data model originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/Experimental/dataDescriptor.java
And also https://github.com/Recoskie/swingIO/blob/Experimental/Descriptor.java
------------------------------------------------------------*/

dataDescriptor.prototype.minDims = [0,0], dataDescriptor.prototype.di = null;

function dataDescriptor( el, io )
{
  this.io = io; var d = this.comp = document.getElementById(el); d.className="vhex"; d.style.overflowY = "auto";
  
  d.innerHTML = "<canvas id=\""+el+"g\" style='position:sticky;top:0px;left:0px;background:#FFFFFF;z-index:-1;'></canvas><div id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d"); this.hide(false);

  //For now we will default to the original "data model place holder".

  d.innerHTML = "Data Model."; d.style.backgroundColor = "#00FF00";

  //Selected element.

  this.sel = 0;
  
  //Scroll.
  
  eval("var t = function(){Ref["+Ref.length+"].sc();}"); d.onscroll=t;

  //clicked data type event.
  
  eval("var t = function(e){Ref["+Ref.length+"].select(e);}");
  
  //If touch screen.
 
  if(touchScreen) { this.comp.ontouchstart = t; } else { this.comp.onmousedown = t; }
  
  //Allows us to referenced the proper component to update on scroll.
  
  Ref[Ref.length] = this;
}

//Scrolling event.

dataDescriptor.prototype.sc = function()
{

}

dataDescriptor.prototype.select = function(e)
{

}

dataDescriptor.prototype.update = function()
{

}

dataDescriptor.prototype.setInspector = function( dInspector ) { this.di = dInspector; }

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

tree.prototype.minDims = [0,0], tree.prototype.selectedNode = null;

function tree(el) { this.comp = document.getElementById(el); this.comp.style.overflow = "auto"; }

//Set the tree nodes.

tree.prototype.set = function(v) { this.comp.innerHTML = "<ul id=\"treeUL\">" + v + "</ul>"; }

//Navigate the tree nodes.

tree.prototype.getNode = function(i)
{
  var r = this.comp.firstChild.children[i]; if(r.children.length >= 2) { r = r.firstChild; } r = r.firstChild;

  r.setArgs = this.setArgs; r.getArgs = this.getArgs; r.setNode = this.setNode; r.getNode = function( i )
  {
    var r = this.parentElement.parentElement.children[1].children[i]; if(r.children.length >= 2) { r = r.firstChild; } r = r.firstChild;
    r.setArgs = this.setArgs; r.getArgs = this.getArgs; r.setNode = this.setNode; r.getNode = this.getNode; return(r);
  }

  return(r);
}

tree.prototype.event = function(){}; tree.prototype.treeClick = function(v,node)
{
  v = v.querySelector("div"); v.self = this; v.setArgs = this.setArgs; v.getArgs = this.getArgs; v.setNode = this.setNode;
  
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

//Modify nodes. Note these methods are referenced from node elements and should not be directly used.

tree.prototype.setArgs = function( arg ) { this.setAttribute("args", arg + ""); }
tree.prototype.getArgs = function() { return( this.getAttribute("args").split(",")); }
tree.prototype.setNode = function( node )
{
  var el = document.createElement("template"); el.innerHTML = node + ""; el = el.content.firstChild;

  if( (t = this.parentElement.parentElement).innerHTML.startsWith("<span") > 0 ){ t.children[1].remove(); }

  this.parentElement.parentElement.replaceChild(el,this.parentElement); if(this.self) { this.self.selectedNode = el.querySelector("div"); }
}

//The tree nodes.

function treeNode(n,args,expand,selected)
{
  var t = 0; for(var i = 0; i < this.fileType.length; i++)
  {
    if( n.substring(n.length-this.fileType[i].length, n.length) == this.fileType[i] )
    {
      t = this.node[i]; n = n.substring(0, n.length-this.fileType[i].length);
    }
  }
  
  this.nodes = ["<li><span "+eventStr+"=\"tree.prototype.treeClick(this,true);\" class=\"node"+t+"\"><div args='"+((args!=null)?args:"")+"' "+(selected?"style='background-color:#9EB0C1;'":"")+">"+n+"</div></span><ul class=\"nested"+(expand?" active":"")+"\">"];
}

treeNode.prototype.add = function(n,args,selected)
{
  if(n instanceof treeNode) { this.nodes[this.nodes.length] = n; n.parentNode = this; return; }
  
  var t = 1; for(var i = 0; i < this.fileType.length; i++)
  {
    if( n.substring(n.length-this.fileType[i].length, n.length) == this.fileType[i] )
    {
      t = this.node[i]; n = n.substring(0, n.length-this.fileType[i].length);
    }
  }
  
  this.nodes[this.nodes.length] = "<li "+eventStr+"='tree.prototype.treeClick(this,false);' class=\"node"+t+"\"><div args='"+((args!=null)?args:"")+"' "+(selected?"style='background-color:#9EB0C1;'":"")+">"+n+"</div></li>";
}

treeNode.prototype.toString = function() { for( var o = "", i = 0; i < this.nodes.length; o += this.nodes[i++] + "" ); return( o + "</ul></li>" ); }

//Shared UI controls.

VHex.prototype.resetDims = dataInspector.prototype.resetDims = tree.prototype.resetDims = dataDescriptor.prototype.resetDims = function() { this.comp.style.minWidth = this.minDims[0] + "px"; this.comp.style.minHeight = this.minDims[1] + "px"; }
VHex.prototype.minWidth = dataInspector.prototype.minWidth = tree.prototype.minWidth = dataDescriptor.prototype.minWidth = function( v ) { return(this.comp.style.minWidth = v || this.comp.style.minWidth); }
VHex.prototype.minHeight = dataInspector.prototype.minHeight = tree.prototype.minHeight = dataDescriptor.prototype.minHeight = function( v ) { return(this.comp.style.minHeight = v || this.comp.style.minHeight); }
VHex.prototype.width = dataInspector.prototype.width = tree.prototype.width = dataDescriptor.prototype.width = function( v ) { return(this.comp.style.width = v || this.comp.style.width); }
VHex.prototype.height = dataInspector.prototype.height = tree.prototype.height = dataDescriptor.prototype.height = function( v ) { return(this.comp.style.height = v || this.comp.style.height); }
VHex.prototype.hide = dataInspector.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; if(this.visible){ this.onseek(this.io); } }
tree.prototype.hide = dataDescriptor.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; }

//64bit lossless base conversion.

Number.prototype.toString64 = function(v32,base)
{
  var o = "", f = this * 4294967296, sec = base**((Math.log(f) / Math.log(base)) & -1), r = 0, r32 = false;
  
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
    for( var s = this.toString(16); s.length < 16; s = "0" + s ); return("0x"+s);
  }
}

//Byte format

Number.prototype.byte = function() { return((s = this.toString(16)).length < 2 ? s = "0" + s : s); }

//Always uppercase.

Number.prototype.toStringB = Number.prototype.toString; Number.prototype.toString = function(b)
{
  return(this.toStringB(b).toUpperCase());
}