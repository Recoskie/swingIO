var dosFont = new FontFace('dos', 'url(swingIO/Font/DOS.ttf)');

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

var VHexRef = [];

function VHex( el, io, v )
{
  this.io = io;
  this.el = document.getElementById(el);
  this.win = this.el.contentWindow;
  
  const doc = this.win.document;
    
  doc.write("<html><body><canvas id='data' style='position: fixed;top:0px;left:0px;width:100%;height:100%;background:#CECECE;z-index: -1;overflow:hidden;'></canvas><div id='size'></div></body></html>");
    
  this.win.stop();
    
  this.size = doc.getElementById("size");
  this.pos = doc.body;
  this.c = doc.getElementById("data");
  this.g = this.c.getContext("2d");
  
  //text column output is optional.
  
  this.text = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)";
  this.addcol = v ? -1 : 42;
  
  eval("var t = function(){parent.VHexRef["+VHexRef.length+"].sc();}");
  
  this.win.addEventListener('scroll', t, false);
  
  dosFont.load().then(function(font){ doc.fonts.add(font); });
  
  //Allows us to referenced the component within frame.
  
  VHexRef[VHexRef.length] = this;
}

VHex.prototype.adjSize = function()
{
  //(x / 16) + ( y - ( y / 16 ) )
  this.setSize( ( this.io.file.size + (this.win.innerHeight*15) ) / 16 );
}

VHex.prototype.sc = function()
{
  this.io.Events = false;
  
  this.io.call = this;
  
  this.io.seek(this.getPos() * 16);
  
  this.io.read(this.getRows() * 16);
  
  this.io.Events = true;
}

//Render the hex editor.

var hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

VHex.prototype.update = function( d )
{
  var g = this.g;
  var width = this.win.innerWidth;
  var height = this.win.innerHeight;
  this.c.width = width; this.c.height = height;
  
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
        val = !isNaN(val) ? val : 0x3F; if( val == 0 || val == 9 || val == 10 || val == 13 ) { val = 0x20; }

        text += String.fromCharCode( val );
      }
    }
    
    if( this.text ) { g.fillText( text, 528, y+15); text = ""; }
    
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
  this.end = (this.text = v) ? 518 : 352;
}

VHex.prototype.hide = function( v )
{
  this.el.style.display = v ? "none" : "";
}

VHex.prototype.getRows = function()
{
  return( this.win.innerHeight / 16 );
}

VHex.prototype.getPos = function() { return( this.pos.scrollTop ); }

VHex.prototype.setSize = function( size ) { this.size.style = "height:" + size + "px;"; }

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