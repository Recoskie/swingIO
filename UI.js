var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/"));

var dosFont = new FontFace('dos', 'url('+path+'/Font/DOS.ttf)'); path = undefined;

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

var VHexRef = [], sBarWidth = null, sBarMax = null, sBarLowLim = null, sBarUpLim = null;

function VHex( el, io, v )
{
  this.io = io; var h = this.comp = document.getElementById(el);

  h.style.position = "relative"; h.style.overflowY = "Scroll"; h.style.overflowX = "hidden";

  h.innerHTML = "<canvas id=\""+el+"g\" style='position:sticky;top:0px;left:0px;background:#CECECE;z-index:-1;'></canvas><div id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d");
  
  //Visible on creation.
  
  this.hide( false );

  //Selected byte positions.

  this.sel = -1; this.sele = -1;
  
  //Find the width of the system scroll bar, and max height of scroll bar.
  //Find the lower and upper limit while scrolling.

  if( sBarWidth == null )
  {
    this.setRows(562949953421312); sBarMax = this.size.clientHeight / 2; sBarWidth = this.comp.offsetWidth - this.comp.clientWidth;
    sBarLowLim = sBarMax * 0.05; sBarUpLim = sBarMax - sBarLowLim;
  }

  //Virtual or offset scroll.

  if( v )
  {
    this.relSize = 562949953421312; this.relDataUp = this.relSize - sBarLowLim; this.rel = true;
    this.adjSize = function() { var s = sBarMax - this.getRows(); this.size.style = "height:" + s + "px;min-height:" + s + "px;"; }
    this.setRows = function(){}; this.sc = this.virtualSc;
  }
  else { this.sc = this.offsetSc; }
  
  //Component min size.
  
  h.style.minWidth = (682 + sBarWidth) + "px"; h.style.minHeight = "256px";
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)"; this.addcol = v ? -1 : 42;

  //Scroll.
  
  eval("var t = function(){VHexRef["+VHexRef.length+"].sc();}"); h.addEventListener("scroll", t, false);

  //Seek byte onclick Event
  
  eval("var t = function(e){VHexRef["+VHexRef.length+"].select(e);}"); this.comp.addEventListener("click", t, false);

  //Load Font.
  
  dosFont.load().then(function(font){ document.fonts.add(font); }); this.setRows(io.file.size);
  
  //Allows us to referenced the proper component to update on scroll.
  
  VHexRef[VHexRef.length] = this;
  
  //Add the component to the IO Event handler.
  
  file.comps[file.comps.length] = this;
}

//Scrolling event.

VHex.prototype.offsetSc = function()
{
  if( this.rel ){ this.adjRelPos(); }

  this.io.Events = false;
  
  this.io.call( this, "update" );

  this.io.seek(this.getPos() * 16);
  
  this.io.read(this.getRows() * 16);
  
  this.io.Events = true;
}

VHex.prototype.virtualSc = function()
{
  this.adjRelPos();

  this.io.Events = false;
  
  this.io.call( this, "update" );

  this.io.seekV(this.getPos() * 16);
  
  this.io.readV(this.getRows() * 16);
  
  this.io.Events = true;
}

//Byte selection event.

VHex.prototype.select = function(e, ref)
{
  var x = ( e.pageX - this.comp.offsetLeft ) - 164, y = ( e.pageY - this.comp.offsetTop ) - 16;

  if( x > 0 && y > 0 )
  {
    var pos = this.getPos() * 16;

    if( x < 355 ) { x = ( x / 22 ) & -1; } else if( this.text && x < 510 ) { x = ( ( x - 365 ) / 9 ) & -1; } else { return; }
    
    y = ( y / 16 ) & -1; pos += y * 16 + x;

    if( !this.virtual ) { this.io.seek( pos ); this.sc(); } else { this.io.seekV( pos ); }
  }
}

//Render the hex editor.

var hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

VHex.prototype.update = function(d)
{
  var g = this.g, width = this.c.width = this.comp.offsetWidth, height = this.c.height = this.comp.offsetHeight;
  
  var data = !this.virtual ? d.data : d.dataV, pos = this.getPos() * 16;
  
  g.font = "16px dos"; g.fillStyle = "#FFFFFF";
  
  g.fillRect(164, 16, this.end, height);
  
  g.stroke();

  if( this.sel >= 0 && this.sele >= 0 ) { this.selection(g, pos); }
  
  g.fillStyle = "#000000";
  
  g.fillText(this.s, this.addcol, 14);
  
  //Columns lines.
  
  for( var x = 166, i = 0; i < 16; x += 22, i++ )
  {
    g.fillText(hexCols[i], x, 14);
    
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

  if( this.sel < this.sele ) { var t = this.sel; this.sel = this.sele; this.sele = t; }

  //Converts offsets to real 2D coordinates.

  var r1 = this.sel & 0xF, y1 = ( this.sel - pos - r1 ) >> 4;
  
  if( y1 < 0 ) { y1 = 0; }

  var r2 = this.sele & 0xF, y2 = ( this.sele - pos - r2 ) >> 4;

  y1 = y1 * 16 + 16; y2 = y2 * 16 + 32;
  
  var x1 = 164 + (r1 * 22), x2 = 164 + ((r2+1) * 22);

  //Single line selection.

  if( (y2-y1) == 16 ) { g.fillRect( x1, y1, x2-x1, y2-y1 ); }

  if( this.text )
  {
    x1 = 529 + (r1 * 9); x2 = 529 + ((r2+1) * 9);

    if( (y2-y1) == 16 ) { g.fillRect( x1-1, y1, x2-x1, y2-y1 ); }
  }

  //Draw the selected offsets.

  g.stroke();
}

//The scroll bar can only be made so big so then we need an way to display very far away addresses.
//So we add a relative position while scrolling to the end based on the max scroll bar size.

VHex.prototype.rel = false; VHex.prototype.relPos = 0; VHex.prototype.relSize = 0; VHex.prototype.oldOff = 0;

//The upper limit for data while scrolling. The lower limit does not need to be calculated.

VHex.prototype.relDataUp = 0;

//Basic UI controls.

VHex.prototype.setText = function( v )
{
  this.comp.style.minWidth = ((v ? 682 : 516) + sBarWidth) + "px"; this.end = (this.text = v) ? 518 : 352;
}

VHex.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; }

VHex.prototype.getRows = function() { return( Math.floor( this.comp.offsetHeight / 16 ) ); }

//It is important that we subtract what is visible from the scroll area otherwise we will scroll past the end.

VHex.prototype.setRows = function( size )
{
  size = Math.floor( size ); size -= this.getRows();

  //Scroll bar can only go so high before it hit's it's limit.

  if( sBarMax != null )
  {
    if( size > sBarMax ) { this.rel = true; this.relSize = size; this.relDataUp = this.relSize - sBarLowLim; size = sBarMax; } else { this.rel = false; }
  }

  //Set size.

  this.size.style = "height:" + size + "px;min-height:" + size + "px;";
}

//We want to keep an extra rows so the user can see the end of the file.

VHex.prototype.adjSize = function() { this.setRows( ( this.io.file.size / 16 ) + 1 ); }

//Get the real position including relative scrolling if active.

VHex.prototype.getPos = function() { return( Math.max(0, this.rel ? this.relPos : this.comp.scrollTop ) ); }

//Adjust relative scrolling or set position directly.

VHex.prototype.setPos = function( offset )
{
  //Relative position.

  if( this.rel )
  {
    //We can directly set the relative position.

    this.relPos = offset;

    //The scroll bar must not pass the rel down position unless rel position is less than rel down.

    if( offset <= sBarLowLim && this.relPos >= sBarLowLim ) { offset = sBarLowLim; }

    //The scroll bar must not pass the rel Up position unless rel position is grater than rel up.

    if( offset >= sBarUpLim && this.relPos <= this.relDataUP ) { offset = sBarUpLim; }
  }

  this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

//Adjust relative positioned scrolling.

VHex.prototype.adjRelPos = function()
{
  var offset = this.comp.scrollTop;

  //Delta difference in scroll bar controls the relative position.

  var delta = offset - this.oldOff; this.relPos += delta;

  //The scroll bar must not pass the rel down position unless rel position is less than rel down data.

  if( offset <= sBarLowLim && this.relPos >= sBarLowLim ) { offset = sBarLowLim; }
  else if( this.relPos <= sBarLowLim ) { this.relPos = offset; }

  //The scroll bar must not pass the rel Up position unless rel position is grater than rel up data.

  if( offset >= sBarUpLim && this.relPos <= this.relDataUp ) { offset = sBarUpLim; }
  else if(this.relPos >= this.relDataUp ) { this.relPos = this.relDataUp + ( offset - sBarUpLim ); }

  //The only time the scroll bar passes the Rel UP or down position is when all that remains is that size of data.

  this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

//The on read IO Event.

VHex.prototype.onread = function( f )
{

}

//The on seek IO Event.

VHex.prototype.onseek = function( f )
{
  if( this.virtual && f.curVra.Mapped ) { this.sele = this.sel = f.virtual; this.update(this.io); } else if( !this.virtual ) { this.sele = this.sel = f.offset; this.update(this.io); }
}

//Address format offsets.

if( Number.prototype.address == null )
{
  Number.prototype.address = function()
  {
    for( var s = this.toString(16).toUpperCase(); s.length < 16; s = "0" + s );
    return("0x"+s);
  }
}

//Byte format

Number.prototype.byte = function()
{
  for( var s = this.toString(16).toUpperCase(); s.length < 2; s = "0" + s );
  return(s);
}