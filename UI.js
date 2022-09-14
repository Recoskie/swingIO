var path = document.currentScript.src; path = path.substring(0, path.lastIndexOf("/"));

var dosFont = new FontFace('dos', 'url('+path+'/Font/DOS.ttf)'); path = undefined;

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

var VHexRef = [], sBarWidth = null;

function VHex( el, io, v )
{
  this.io = io; var h = this.comp = document.getElementById(el);

  h.style.position = "relative"; h.style.overflowY = "Scroll";

  h.innerHTML = "<canvas id=\""+el+"g\" style='position: sticky;top:0px;left:0px;width: 100%;height:100%;background:#CECECE;z-index:-1;'></canvas><div id=\""+el+"s\"></div>";

  this.size = document.getElementById(el+"s"); this.c = document.getElementById(el+"g"); this.g = this.c.getContext("2d");
  
  //Visible on creation.
  
  this.hide( false );
  
  //Find the width of the system scroll bar.

  if( sBarWidth == null )
  {
    this.setSize(this.comp.offsetHeight + 16);
    sBarWidth = this.comp.offsetWidth - this.comp.clientWidth;
    this.setSize(0);
  }
  
  //Component min size.
  
  h.style.minWidth = (682 + sBarWidth) + "px"; h.style.minHeight = "256px";
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)"; this.addcol = v ? -1 : 42;

  //Scroll.
  
  eval("var t = function(){VHexRef["+VHexRef.length+"].sc("+VHexRef.length+");}"); h.addEventListener('scroll', t, false); this.setSize(io.file.size);

  //Load Font.
  
  dosFont.load().then(function(font){ document.fonts.add(font); });
  
  //Allows us to referenced the proper component to update on scroll.
  
  VHexRef[VHexRef.length] = this;
  
  //Add the component to the IO Event handler.
  
  file.comps[file.comps.length] = this;
}

//Scrolling event.

VHex.prototype.sc = function()
{
  this.io.Events = false;
  
  this.io.call( this, "update" );

  this.io.seek(Math.floor(this.getPos()) * 16);
  
  this.io.read(Math.floor(this.getRows()) * 16);
  
  this.io.Events = true;
}

//Render the hex editor.

var hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

VHex.prototype.update = function( d )
{
  var g = this.g, width = this.c.width = this.c.offsetWidth, height = this.c.height = this.c.offsetHeight;
  
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
      val = d.data[i1+i2]; g.fillText(!isNaN(val) ? val.byte() : "??", x, y+13);
      
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
  
  var pos = d.offset;
  
  height -= 16; for( var i = 0; i < height; i += 16 )
  {
    g.fillText((pos + i).address(), 0, i+29);
  }
  
  g.stroke();
}

VHex.prototype.setText = function( v )
{
  this.comp.style.minWidth = ((v ? 682 : 516) + sBarWidth) + "px";
  this.end = (this.text = v) ? 518 : 352;
}

VHex.prototype.hide = function( v ) { this.visible = !v; this.comp.style.display = v ? "none" : ""; }

VHex.prototype.getRows = function() { return( this.comp.offsetHeight / 16 ); }

VHex.prototype.getPos = function() { return( this.comp.scrollTop ); }

VHex.prototype.setPos = function( offset ) { this.comp.scrollTo( 0, offset ); }

VHex.prototype.setSize = function( size ) { this.size.style = "height:" + size + "px;min-height:"+size+"px;"; }

VHex.prototype.checkSize = function() { return( (this.size.clientHeight+"px") == this.size.style.height ); }

VHex.prototype.adjSize = function() { this.setSize( ( this.io.file.size - this.comp.offsetHeight + 48 ) / 16 ); }

//The on read IO Event.

VHex.prototype.onread = function() { }

//The on seek IO Event.

VHex.prototype.onseek = function() { }

//Address format offsets.

Number.prototype.address = function()
{
  for( var s = this.toString(16).toUpperCase(); s.length < 16; s = "0" + s );
  return("0x"+s);
}

//Byte format

Number.prototype.byte = function()
{
  for( var s = this.toString(16).toUpperCase(); s.length < 2; s = "0" + s );
  return(s);
}