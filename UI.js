var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/"));

var dosFont = new FontFace('dos', 'url('+path+'/Font/DOS.ttf)'); path = undefined;

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

var VHexRef = [], sBarWidth = null, sBarMax = null;

function VHex( el, io, v )
{
  this.io = io; var h = this.comp = document.getElementById(el);

  h.style.position = "relative"; h.style.overflowY = "Scroll"; h.style.overflowX = "hidden";

  h.innerHTML = "<canvas id=\""+el+"g\" style='position:sticky;top:0px;left:0px;background:#CECECE;z-index:-1;'></canvas><div id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d");
  
  //Visible on creation.
  
  this.hide( false );
  
  //Find the width of the system scroll bar, and max height of scroll bar.

  if( sBarWidth == null )
  {
    this.setRows(562949953421312); sBarMax = this.size.clientHeight / 2; sBarWidth = this.comp.offsetWidth - this.comp.clientWidth;
    this.relDOWN = sBarMax * 0.05; this.relUP = sBarMax * 0.05;
  }

  //The virtual address mode is not size adjustable as it is always 562949953421312 * 16.

  if( v ) { this.setRows(562949953421312); this.setRows = function() {}; this.sc = this.virtualSc; } else { this.sc = this.offsetSc; }
  
  //Component min size.
  
  h.style.minWidth = (682 + sBarWidth) + "px"; h.style.minHeight = "256px";
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)"; this.addcol = v ? -1 : 42;

  //Scroll.
  
  eval("var t = function(){VHexRef["+VHexRef.length+"].sc("+VHexRef.length+");}"); h.addEventListener('scroll', t, false); this.setRows(io.file.size);

  //Load Font.
  
  dosFont.load().then(function(font){ document.fonts.add(font); });
  
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

  this.io.seek(Math.floor(this.getPos()) * 16);
  
  this.io.read(Math.floor(this.getRows()) * 16);
  
  this.io.Events = true;
}

VHex.prototype.virtualSc = function()
{
  this.adjRelPos();

  this.io.Events = false;
  
  this.io.call( this, "update" );

  this.io.seekV(Math.floor(this.getPos()) * 16);
  
  this.io.readV(Math.floor(this.getRows()) * 16);
  
  this.io.Events = true;
}

//Render the hex editor.

var hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

VHex.prototype.update = function( d )
{
  var g = this.g, width = this.c.width = this.comp.offsetWidth, height = this.c.height = this.comp.offsetHeight, data = !this.virtual ? d.data : d.dataV;
  
  g.font = "16px dos"; g.fillStyle = "#FFFFFF";
  
  g.fillRect(164, 16, this.end, height);
  
  g.stroke();
  
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
  
  var pos = !this.virtual ? d.offset : d.virtual;
  
  height -= 16; for( var i = 0; i < height; i += 16 )
  {
    g.fillText((pos + i).address(), 0, i+29);
  }
  
  g.stroke();
}

//The scroll bar can only be made so big so then we need an way to display very far away addresses.
//So we add a relative position while scrolling to the end based on the max scroll bar size.

VHex.prototype.rel = false; VHex.prototype.relPos = 0; VHex.prototype.relSize = 0;
VHex.prototype.oldOff = 0;

//The upper and lower limit for relative scroll. The scroll bar does not pass these values until we approach remaining data with theses relative values.

VHex.prototype.relUP = 0x1000; VHex.prototype.relDOWN = 0x1000;

//Basic UI controls.

VHex.prototype.setText = function( v )
{
  this.comp.style.minWidth = ((v ? 682 : 516) + sBarWidth) + "px"; this.end = (this.text = v) ? 518 : 352;
}

VHex.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; }

VHex.prototype.getRows = function() { return( this.comp.offsetHeight / 16 ); }

//It is important that we subtract what is visible from the scroll area otherwise we will scroll past the end.

VHex.prototype.setRows = function( size )
{
  size -= this.getRows();

  //Scroll bar can only go so high before it hit's it's limit.

  if( sBarMax != null ) { if( size > sBarMax ){ this.rel = true; this.relSize = size; size = sBarMax; } else { this.rel = false; } }

  //Set size.

  this.size.style = "height:" + size + "px;min-height:" + size + "px;";
}

//We want to keep three extra rows so the user can see the end of the file.

VHex.prototype.adjSize = function() { this.setRows( ( this.io.file.size / 16 ) + 3 ); }

//Get the real position including relative scrolling if active.

VHex.prototype.getPos = function() { return( this.rel ? this.relPos : this.comp.scrollTop ); }

//Adjust relative scrolling or set position directly.

VHex.prototype.setPos = function( offset )
{
  //Relative position.

  if( this.rel )
  {
    //We can directly set the relative position.

    this.relPos = offset;

    //The scroll bar must not pass the rel down position unless rel position is less than rel down.

    if( offset <= this.relDOWN && this.relPos >= this.relDOWN ) { offset = this.relDOWN; }

    //The scroll bar must not pass the rel Up position unless rel position is grater than rel up.

    if( offset >= ( sBarMax - this.relUP ) && this.relPos <= ( this.relSize - this.relUP ) ) { offset = sBarMax - this.relUP; }
  }

  this.comp.scrollTo( 0, offset );
}

//Adjust relative positioned scrolling.

VHex.prototype.adjRelPos = function()
{
  offset = this.comp.scrollTop;

  //Delta difference in scroll bar controls the relative position.

  var delta = offset - this.oldOff; this.relPos += delta;

  //The scroll bar must not pass the rel down position unless rel position is less than rel down.

  if( offset <= this.relDOWN && this.relPos >= this.relDOWN ) { offset = this.relDOWN; }
  else if( this.relPos <= this.relDOWN )
  {
    this.relPos = offset;
  }

  //The scroll bar must not pass the rel Up position unless rel position is grater than rel up.

  if( offset >= ( sBarMax - this.relUP ) && this.relPos <= ( this.relSize - this.relUP ) ) { offset = sBarMax - this.relUP; }
  else if(this.relPos >= ( this.relSize - this.relUP ))
  {
    this.relPos = ( this.relSize - this.relUP ) + ( offset - ( sBarMax - this.relUP ) );
  }

  //The only time the scroll bar passes the Rel UP or down position is when all that remains is that size of data.

  this.comp.scrollTo( 0, offset ); this.oldOff = offset;
}

//The on read IO Event.

VHex.prototype.onread = function() { }

//The on seek IO Event.

VHex.prototype.onseek = function() { }

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