var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/")), Ref = [], tCheck = false;

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
This is a specialized UI function that is called when components resize and more data needs to be displayed.
Only one read operation can occur at a time so it is important that we iterate through them and load in data needed.
All components that already have the data necessary are updated right away, while components that do not are updated after data is read in updateV.
Note that it would make sense to group together the loose public variables that are shared into one object.
------------------------------------------------------------*/

var vList = []; async function validate()
{
  if( vList.length > 0 ){ return; }
  
  vList.pos = 0; for( var i = 0, r = Ref[0]; i < Ref.length; r=Ref[++i] )
  {
    if(r instanceof VHex && r.visible)
    {
      //We must update the scroll bar any time height does not match.

      if( r.c.height != r.comp.clientHeight ){ r.adjSize(); }

      //Does not match the memory buffer, then we must reload data and render the output.

      if( (r.getPos() << 4) != (r.virtual ? r.io.dataV.offset : r.io.data.offset) || (((( r.comp.clientHeight >> 4 ) << 4 ) > (r.virtual ? r.io.dataV.length : r.io.data.length))) )
      {
        vList[vList.length] = {virtual:r.virtual,pos:r.getPos() << 4, size:( r.comp.clientHeight >> 4 ) << 4, el:i};
      }

      //Aligns in memory buffer but needs to draw more rows.

      else if( r.c.height>>4 < r.comp.clientHeight>>4 ) { r.update(); }
    }
    else if(r instanceof dataDescriptor)
    {
      //We must update the scroll bar any time height does not match.

      if( r.c.height != r.comp.clientHeight ){ r.adjSize(); }

      if( r.update == r.dataCheck )
      {
        r.minRows = Math.min( r.data.rows, ((r.comp.clientHeight / 16) + 0.5)&-1 );
        r.curRow = Math.max(Math.min(r.comp.scrollTop,r.data.rows), 0) & -1, r.endRow = Math.min( r.curRow + r.minRows, r.data.rows ) & - 1;
    
        //Data within the current buffer area.
    
        var dPos = (r.data.offset + r.data.relPos[r.curRow]), data = r.data.bytes(r.curRow,r.endRow);
     
        if(r.io.data.offset <= dPos && (r.io.data.offset-dPos+r.io.data.length) >= data) { r.dataUpdate(); }
    
        //Else we need to load the data we need before updating the component. This is least likely to happen.

        vList[vList.length] = {virtual:false,pos:dPos, size:data, el:i};
      }
      else { r.update(); }
    }
  }

  //Begin reading the data for the first component that needs to update.

  if( vList.length > 0 ) { setTimeout(function() { updateV(); }, 0); }
}

async function updateV()
{
  if( vList.pos > 0 ) { Ref[vList[vList.pos-1].el].update(); }

  if( vList.pos < vList.length )
  {
    var io = Ref[vList[vList.pos].el].io; io.buf = vList.pos == 0 ? vList[vList.pos].size : Math.max(io.buf, vList[vList.pos].size);
    
    //It is very important that we wait till other IO processing finishes before validating the data and UI components.
    
    if ( (io.fr.readyState | io.frv.readyState) & 1 ) { setTimeout(function() { updateV(); }, 0); } else
    {
      io.call(this, "updateV");
    
      if(vList[vList.pos].virtual) { io.seekV(vList[vList.pos].pos); io.readV(vList[vList.pos].size); }
      else { io.seek(vList[vList.pos].pos); io.read(vList[vList.pos].size); }

      //This acts as a fallback if operation completed else where or is cached.
    
      vList.pos += 1; setTimeout(function() { updateV(); }, 0);
    }
  }
  else { vList = []; }
}

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
    this.setRows(562949953421312); var o = this.size.clientHeight;
    
    //Firefox sets height 0 when too large. In this case we must calculate max height.

    if( o == 0 )
    {
      this.setSize = function( s ){ this.size.style = "height:"+s+"px;min-height:"+s+"px;border:0;"; }
      this.setSize(n = 1); while( this.size.clientHeight != 0 ){ this.setSize(n <<= 1); } o = n >>= 1; n >>= 1; while( n > 0 ){ this.setSize(o | n); if( this.size.clientHeight != 0 ) { o |= n; } n >>= 1; }
      n = this.setSize = undefined;
    }
    
    VHex.prototype.sBarMax = o / 2; o = undefined;
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
  
  eval("var t = function(e){if(!tCheck){Ref["+Ref.length+"].select(e);};tCheck=(e.type=='touchstart');}");
  
  this.comp.onmousedown = this.comp.ontouchstart = t;

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

VHex.prototype.update = function()
{
  var g = this.g, height = this.c.height = this.comp.clientHeight; this.c.width = this.comp.clientWidth;
  
  var data = !this.virtual ? this.io.data : this.io.dataV, pos = !this.virtual ? this.io.data.offset : this.io.dataV.offset;
  
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

VHex.prototype.setText = function( v ) { this.minDims = [(v ? 682 : 516) + this.sBarWidth, 256]; this.end = (this.text = v) ? 518 : 352; this.resetDims(); if( this.visible ) { this.update(this.io); } }

VHex.prototype.getRows = function() { return( Math.floor( this.comp.clientHeight / 16 ) ); }

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

  if( offset <= this.sBarLowLim && this.relPos >= this.sBarLowLim ) { offset = this.sBarLowLim; }
  else if( this.relPos <= this.sBarLowLim ) { this.relPos = offset; }

  //The scroll bar must not pass the rel Up position unless rel position is grater than rel up data.

  if( offset >= this.sBarUpLim && this.relPos <= this.relDataUp ) { offset = this.sBarUpLim; }
  else if(this.relPos >= this.relDataUp ) { this.relPos = this.relDataUp + ( offset - this.sBarUpLim ); }

  //The only time the scroll bar passes the Rel UP or down position is when all that remains is that size of data.

  this.sc = this.blockSc; this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

VHex.prototype.onread = function() { }

//Select the byte we have seeked to in the IO system. If the byte is outside the hex editor, then update the position.

VHex.prototype.onseek = function( f )
{
  var vs = (this.oldOff = (this.relPos + this.comp.scrollTop)) * 16, ve = vs + f.buf, pos = this.virtual ? f.virtual : f.offset;

  this.sele = ( this.sel = pos ) + (this.slen > 0 ? this.slen - 1 : 0);
  
  if( pos > ve || pos < vs ) { this.sc = this.blockSc; this.comp.scrollTo( 0, pos >> 4 ); }
    
  if( this.rel ) { this.adjRelPos(); } this.update(f);
}

/*------------------------------------------------------------
This is a web based version of the Data type inspector originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/dataInspector.java
------------------------------------------------------------*/

dataInspector.prototype.dType = ["Binary (8 bit)","Int8","UInt8","Int16","UInt16","Int32","UInt32","Int64","UInt64","Float32","Float64","Char8","Char16","String8","String16","Use No Data type"];
dataInspector.prototype.dLen = [1,1,1,2,2,4,4,8,8,4,8,1,2,0,0,-1], dataInspector.prototype.minDims = null, dataInspector.prototype.minChar = null;

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
  
  d.className = "dataInspec";
  
  var out = "<table style='table-layout:fixed;width:0px;height:0px;'><tr><td>Data Type</td><td>Value</td></tr>", event = "";
  
  this.out = []; for(var i = 0; i < this.dType.length; i++) { event = "='event.preventDefault();Ref["+Ref.length+"].setType("+i+");'"; out += "<tr ontouchstart"+event+" onmousedown"+event+"><td>" + this.dType[i] + "</td><td>?</td></tr>"; }
  
  event = "onclick='Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);'";
  
  out += "<tr><td colspan='2'><fieldset><legend>Byte Order</legend><span><input type='radio' "+event+" name='"+el+"o' value='0' checked='checked' />Little Endian</span><span style='width:50%;'><input type='radio' "+event+" name='"+el+"o' value='1' />Big Endian</span></fieldset></td><tr>";
  
  event = "onclick='Ref["+Ref.length+"].base = this.value;Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);'";
  
  out += "<tr><td colspan='2'><fieldset><legend>Integer Base</legend><span><input type='radio' "+event+" name='"+el+"b' value='2' />Native Binary</span><span><input type='radio' "+event+" name='"+el+"b' value='8' />Octal</span><span><input type='radio' "+event+" name='"+el+"b' value='10' checked='checked' />Decimal</span><span><input type='radio' "+event+" name='"+el+"b' value='16' />Hexadecimal</span></fieldset></fieldset></td><tr>";
  
  out += "<tr><td colspan='2'><fieldset><legend>String Char Length</legend><input type='number' min='0' max='65536' step='1' style='width:100%;' onchange='Ref["+Ref.length+"].dLen[14] = (Ref["+Ref.length+"].dLen[13] = this.value = Ref["+Ref.length+"].strLen = Math.min( this.value, 65536)) << 1;Ref["+Ref.length+"].onseek(Ref["+Ref.length+"].io);' value='0' /></fieldset></td><tr>";
  
  d.innerHTML = out;
  
  //Byte order control.
  
  this.order = ([].slice.call(document.getElementsByName(el+"o"), 0)).reverse();
  
  //Setup data type outputs.
  
  this.td = d.getElementsByTagName("table")[0];
  
  for(var i = 1; i <= this.dType.length; i++) { this.out[this.out.length] = this.td.rows[i].cells[1]; }
  
  this.base = 10; this.strLen = 0;
  
  this.out[15].innerHTML = ""; this.setType(15, 0);
  
  //Visible on creation.
  
  this.hide( false );
  
  //Component min size.
  
  var t = d.getElementsByTagName("table")[0];
  
  if(this.minDims == null) { dataInspector.prototype.minDims = [d.getElementsByTagName("fieldset")[1].clientWidth+16, t.clientHeight+32]; }
  
  t.style.minWidth=d.style.minWidth=this.minDims[0]; d.style.minHeight=this.minDims[1];
  
  t.style.width = "100%"; t.style.height = "100%"; t = undefined;
  
  //Allows us to referenced the proper component on update.
  
  Ref[Ref.length] = this;
  
  //Add the component to the IO Event handler.
  
  io.comps[io.comps.length] = this;
}

dataInspector.prototype.setType = function(t,order)
{
  this.order[order&-1].checked = true;
  
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
    
    if( this.order[1].checked )
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

  for( var i = 0; i < maxCharLen; i++ ){ c += String.fromCharCode(f.data[rel+i]); } this.out[13].innerHTML = c; c = "";

  if( this.order[1].checked )
  {
    for( var i = 0, e = maxCharLen << 1; i < e; i+=2 ) { c += String.fromCharCode((f.data[rel+i+1]<<8)+f.data[rel+i]); }
  }
  else
  {
    for( var i = 0, e = maxCharLen << 1; i < e; i+=2 ) { c += String.fromCharCode((f.data[rel+i]<<8)+f.data[rel+i+1]); }
  }

  this.out[14].innerHTML = c; c = undefined;
}

dataInspector.prototype.addEditor = function( vhex ) { this.editors[this.editors.length] = vhex; }

/*------------------------------------------------------------
This is a web based version of the data model originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/Experimental/dataDescriptor.java
And also https://github.com/Recoskie/swingIO/blob/Experimental/Descriptor.java
------------------------------------------------------------*/

dataDescriptor.prototype.DType = ["Bit8",,"Int8",,"UInt8",,"Int16","LInt16","UInt16","LUInt16","Int32","LInt32","UInt32","LUInt32","Int64","LInt64","UInt64","LUInt64","Float32","LFloat32","Float64","LFloat64", "Char8",,"Char16","LChar16","String8",,"String16","LString16","Other",,"Array"];
dataDescriptor.prototype.di = null, dataDescriptor.prototype.data = new Descriptor([]);
dataDescriptor.prototype.minDims = null, dataDescriptor.prototype.textWidth = [], dataDescriptor.prototype.minHex = 28;

function dataDescriptor( el, io )
{
  this.io = io; var d = this.comp = document.getElementById(el); d.className="vhex"; d.style.overflowY = "auto";
  
  d.innerHTML = "<canvas id=\""+el+"g\" style='position:sticky;top:0px;left:0px;background:#FFFFFF;z-index:-1;'></canvas><div style='border: 0;' id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d"); this.hide(false);

  //We should only ever measure this once.

  this.g.font = "14px " + this.g.font.split(" ")[1]; if( this.textWidth[0] == null )
  {
    dataDescriptor.prototype.textWidth = [this.g.measureText("Use").width>>1,this.g.measureText("Raw Data").width>>1,this.g.measureText("Data Type").width>>1,
    this.g.measureText("Operation").width>>1, this.g.measureText("Address").width>>1];
  
    //Minium width of hex characters. We do not want to translate a lot more hex values that what will fit into a table cell.
    
    var w = Infinity; for( var i = 0; i < 16; w = Math.min(w, this.g.measureText((( i < 10 ) ? i : String.fromCharCode(55 + i)) + "").width), i++ );
    dataDescriptor.prototype.minHex = w * 2 + this.g.measureText(" ").width - 2; w = undefined;
  }

  //Component minimum size.

  if( this.minDims == null ){ dataDescriptor.prototype.minDims = [((this.textWidth[0]+this.textWidth[1]+this.textWidth[2])<<1) + 16, 192]; } this.resetDims();

  //Selected element.

  this.selectedRow = -1;
  
  //Scroll.
  
  eval("var t = function(){Ref["+Ref.length+"].sc();}"); d.onscroll=t;

  //clicked data type event.
  
  eval("var t = function(e){if(!tCheck){Ref["+Ref.length+"].select(e);};tCheck=(e.type=='touchstart');}");
  
  //If touch screen.
 
  this.comp.ontouchstart = this.comp.onmousedown = t;
  
  //Allows us to referenced the proper component to update on scroll.
  
  Ref[Ref.length] = this;
}

//Scrolling event.

dataDescriptor.prototype.sc = function() { this.update(); }

dataDescriptor.prototype.select = function(e)
{
  this.selectedRow = (this.comp.scrollTop + ( (((e.pageY || e.touches[0].pageY) - this.comp.offsetTop ) ) >> 4 ))&-1; if( this.selectedRow < 1 || this.data.rows == 0 ) { return; }
  this.selectedRow = Math.min( this.selectedRow, this.data.rows ) - 1;

  //Data types.

  if( this.update == this.dataCheck )
  {
    this.di.setType( this.data.data[this.selectedRow] >> 1, (this.data.data[this.selectedRow] & 1) == 1 ); this.data.source[this.data.event]( this.selectedRow );
  
    this.io.seek(this.data.offset + this.data.relPos[this.selectedRow]);
  }

  //Processor core.

  else
  {
    var r = 0; r += this.selectedRow;
    
    if( r < ( this.data.linear.length >> 1 ) )
    {
      this.coreDisLoc(this.data.linear[r],false);
    }
    else if( ( r -= ( this.data.linear.length >> 1 ) ) < this.data.crawl.length )
    {
      this.coreDisLoc(this.data.crawl[r],true);
    }
    else
    {
      r -= this.data.crawl.length; r = r << 1; this.io.seekV( this.data.data_off[r] );
    }
  }
  
  this.update();
}

//Update is changeable based on if we are working with processor core data, or just file data.
//Before updating we must check if the buffer data is in the correct offset.

dataDescriptor.prototype.update = dataDescriptor.prototype.dataCheck = function()
{
  this.minRows = Math.min( this.data.rows, ((this.comp.clientHeight / 16) + 0.5)&-1 );
  this.curRow = Math.max(Math.min(this.comp.scrollTop,this.data.rows), 0) & -1, this.endRow = Math.min( this.curRow + this.minRows, this.data.rows ) & - 1;

  //Data within the current buffer area.

  var dPos = (this.data.offset + this.data.relPos[this.curRow]), data = this.data.bytes(this.curRow,this.endRow);
 
  if(this.io.data.offset <= dPos && (this.io.data.offset-dPos+this.io.data.length) >= data) { this.dataUpdate(); }

  //Else we need to load the data we need before updating the component. This is least likely to happen.

  else { this.io.call( this, "dataUpdate" ); this.io.seek(dPos); this.io.read(data); }
}

//Update output as data model.

dataDescriptor.prototype.dataUpdate = function()
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
 
  //Fill in the columns based on the current position of the scroll bar.
 
  for( var i = this.curRow, posY = 32; i < this.endRow; posY += 16, i++ )
  {
    //Selected row. Event handling not ready yet.

    if( i == this.selectedRow ){ g.stroke(); g.fillStyle = "#9EB0C1"; g.fillRect(0, posY - 16, width, 16); g.stroke(); g.fillStyle = "#000000"; }

    //Data type description.
 
    g.drawString( this.data.des[i], 2, posY - 3, cols-4 );
 
    //Convert data to bytes.
 
    var pos = (this.data.relPos[i]+this.data.offset)-this.io.data.offset, size = this.data.relPos[i+1]-this.data.relPos[i]; size = Math.min( cols / this.minHex, size);
      
    for(var i2 = 0; i2 < size; str+=(this.io.data[pos+i2]&-1).byte()+((i2<(size-1))?" ":""), i2++);

    g.drawString(str, cols + 2, posY - 3, cols-4); str = "";

    //Show the related data type.
 
    g.fillText( this.DType[this.data.data[i]], ( cols << 1 ) + 2, posY - 3 ); g.moveTo(0, posY); g.lineTo(width, posY);
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

//Set the data model.

dataDescriptor.prototype.setDescriptor = function( d )
{
  this.update = this.dataCheck; this.data = d; this.selectedRow = -1;
  
  this.adjSize(); this.comp.scrollTo(0,0); this.data.source[this.data.event]( -1 );

  this.update();
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

function dataType(str,Type) { this.des = str; this.type = Type; }

//The position we wish to style binary data.

Descriptor.prototype.offset = 0;

//Data inspector types, and byte length size.
//Note that this could be shrunk down by better relating the data inspector types and names array.

Descriptor.prototype.Bytes = dataInspector.prototype.dLen.slice();Descriptor.prototype.Bytes[13]=Descriptor.prototype.Bytes[14]=Descriptor.prototype.Bytes[15]=-1;Descriptor.prototype.Bytes[16]=-2;
for(var i=0;i<dataDescriptor.prototype.DType.length;dataDescriptor.prototype.DType[i]&&(Descriptor[dataDescriptor.prototype.DType[i]]=i),i++);

//Construct the data descriptor.

function Descriptor(data)
{
  //Stores the data types that will be rendered.

  this.des = []; this.data = [];
  
  //rows array starts at. This information is used to subtract row to find the individual items in rel pos.

  this.relPos = []; this.arPos = [];

  //Number of rows that this descriptor will display. Note when I add in set methods this value will change if array sizes change.
  
  this.rows = 0;

  //begin organizing data for optimized display.

  var defArray = false, arrayEl = 0, arraySize = 0, arrayLen = 0, length = 0;

  for( var i = 0, b = 0; i < data.length; i++ )
  {
    this.des[i] = data[i].des; this.data[i] = data[i].type; b = this.Bytes[this.data[i]>>1];
      
    if( b == -1 ){ i += 1; this.data[i] = data[i].type; b = data[i]; } else if( defArray = ( b == -2 ) )
    {
      this.arPos[this.arPos.length] = rows; this.data[i+1] = data[i+1].type; this.data[i+2] = data[i+2].type;

      rows += ( (arrayEl = this.data[i+1]) + 1 ) * (arraySize = this.data[i+2]); i += 2;
    }
      
    if( !defArray ) { this.relPos[this.relPos.length]=length; length += b; this.rows += 1; }
    else
    {
      arrayLen += b; if(!(defArray = (arrayEl-- > 0))) { length += arrayLen * arraySize; }
    }
  }
    
  this.relPos[this.relPos.length]=length;
    
  //Event handler for when data descriptor is set or user clicks on a property or value.
  
  this.Event = function(){}; this.event="Event"; this.source = this;
}

//Calc number of bytes that need to be read to display rows.
//For now we will not involve array types.

Descriptor.prototype.bytes = function(r1,r2) { return( this.relPos[r2] - this.relPos[r1] ); }

//Sets the method that is called when user clicks a data type.

Descriptor.prototype.setEvent = function( s, e ) { this.event = e; this.source = s; }

//The total length of the data.

Descriptor.prototype.length = function() { return( this.relPos[this.relPos.length - 1] ); }

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

function treeNode(n,args,expand)
{
  var t = 0; for(var i = 0; i < this.fileType.length; i++)
  {
    if( n.substring(n.length-this.fileType[i].length, n.length).toLowerCase() == this.fileType[i] )
    {
      t = this.node[i]; n = n.substring(0, n.length-this.fileType[i].length);
    }
  }
  
  this.nodes = ["<li><span onclick=\"tree.prototype.treeClick(this,true);\" class=\"node"+t+"\"><div args='"+((args!=null)?args:"")+"'>"+n+"</div></span><ul class=\"nested"+(expand?" active":"")+"\">"];
}

treeNode.prototype.add = function(n,args)
{
  if(n instanceof treeNode) { this.nodes[this.nodes.length] = n; n.parentNode = this; return; }
  
  var t = 1; for(var i = 0; i < this.fileType.length; i++)
  {
    if( n.substring(n.length-this.fileType[i].length, n.length).toLowerCase() == this.fileType[i] )
    {
      t = this.node[i]; n = n.substring(0, n.length-this.fileType[i].length);
    }
  }
  
  this.nodes[this.nodes.length] = "<li onclick='tree.prototype.treeClick(this,false);' class=\"node"+t+"\"><div args='"+((args!=null)?args:"")+"'>"+n+"</div></li>";
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