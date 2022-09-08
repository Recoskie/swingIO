var dosFont = new FontFace('dos', 'url(swingIO/Font/DOS.ttf)');

/*------------------------------------------------------------
This is a web based version of VHex originally designed to run in Java.
See https://github.com/Recoskie/swingIO/blob/master/VHex.java
------------------------------------------------------------*/

function VHex( el, v, event )
{
  this.el = document.getElementById(el);
  this.win = this.el.contentWindow;
  
  const doc = this.win.document;
    
  doc.write("<html><body><canvas id='data' style='position: fixed;top:0px;left:0px;width:100%;height:100%;background:#CECECE;z-index: -1;overflow:hidden;'></canvas><div id='size'></div></body></html>");
    
  this.win.stop();
    
  this.size = doc.getElementById("size");
  this.c = doc.getElementById("data");
  this.g = this.c.getContext("2d");
  this.pos = doc.body;
  
  //text column output is optional.
  
  this.textCol = true; this.end = 518;
  
  //virtual or file offset view.
  
  this.s = (this.virtual = v) ? "Virtual Address (h)" : "Offset (h)";
  this.addcol = v ? -1 : 42;
  
  this.win.addEventListener('scroll', event, false);
  
  dosFont.load().then(function(font){ doc.fonts.add(font); });
}

//Render the hex editor.

var hexCols = ["00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F"];

VHex.prototype.update = function( data )
{
  var g = this.g;
  var width = this.win.innerWidth;
  var height = this.win.innerHeight
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
  
  if( this.textCol ) { g.fillText("Text", 584, 14); }
  
  //Rows.
  
  for( var y = 16, i1 = 0, text = ""; y < height; y += 16, i1 += 16 )
  {
    for( var x = 166, i2 = 0, val = 0; i2 < 16; x += 22, i2++ )
    {
      val = data[i1+i2]; g.fillText(!isNaN(val) ? val.byte() : "??", x, y+13);
      
      if( this.textCol )
      { 
        val = !isNaN(val) ? val : 0x3F; if( val == 9 || val == 10 || val == 13 ) { val = 0x20; }

        text += String.fromCharCode( val );
      }
    }
    
    if( this.textCol ) { g.fillText( text, 528, y+13); text = ""; }
    
    g.moveTo(164, y); g.lineTo(514, y);
  }
  
  //Address and offset column.
  
  g.fillRect(0, 16, 164, height);
  
  g.stroke(); g.fillStyle = "#FFFFFF";
  
  var pos = this.getPos(); pos = (pos < 0 ? 0 : pos) * 16;
  
  height -= 16; for( var i = 0; i < height; i += 16 )
  {
    g.fillText((pos + i).address(), 0, i+32);
  }
  
  g.stroke();
}

VHex.prototype.setText = function( v )
{
  this.end = (this.textCol = v) ? 518 : 352;
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