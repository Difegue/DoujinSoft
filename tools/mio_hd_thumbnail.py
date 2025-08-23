# -*- coding: UTF-8 -*-
#Python 2 + PIL
#the very basics of the mio format(art location + reading) RE'd from Warioware DIY Editor (Untitled1Q.jar)
from PIL import Image
from io import BytesIO
import glob, os, struct, random

randseed = None#set to None for for different outcomes each time for objects that are placed within an area
pal = [(0x00,0x00,0x00,0x00), (0x00,0x00,0x00,0xFF), (0xFF,0xDF,0x9E,0xFF), (0xFF,0xAE,0x34,0xFF),
       (0xC7,0x4D,0x00,0xFF), (0xFF,0x00,0x00,0xFF), (0xCF,0x6D,0xEF,0xFF), (0x14,0xC7,0xCF,0xFF),
       (0x2C,0x6D,0xC7,0xFF), (0x0C,0x96,0x55,0xFF), (0x75,0xD7,0x3C,0xFF), (0xFF,0xFF,0x5D,0xFF),
       (0x7D,0x7D,0x7D,0xFF), (0xC7,0xC7,0xC7,0xFF), (0xFF,0xFF,0xFF,0xFF), (0xFF,0xFF,0xFF,0xFF)]

####from https://code.activestate.com/recipes/113799-bit-field-manipulation/
class bf(object):
    def __init__(self,value=0):
        self._d = value

    def __getitem__(self, index):
        return (self._d >> index) & 1 

    def __setitem__(self,index,value):
        value    = (value&1L)<<index
        mask     = (1L)<<index
        self._d  = (self._d & ~mask) | value

    def __getslice__(self, start, end):
        mask = 2L**(end - start) -1
        return (self._d >> start) & mask

    def __setslice__(self, start, end, value):
        mask = 2L**(end - start) -1
        value = (value & mask) << start
        mask = mask << start
        self._d = (self._d & ~mask) | value
        return (self._d >> start) & mask

    def __int__(self):
        return self._d
####

def tosigned(num): #takes 10 bit number and converts to signed
    return (num - (1<<10)) if (num & (1<<9)) else num

def parseimagedata(image, data, width, height, someval):
    for y in xrange(height):
        for x in xrange(width):
            offset = 0
            offset += (y / 8) * someval
            offset += (x / 8) * 32
            offset += (y % 8) * 4
            offset += (x % 8) / 2
            mybyte = data[offset]
            if (x % 2 != 0):
                color = pal[ord(mybyte) >> 4]
            else:
                color = pal[ord(mybyte) & 0xF]
            image.putpixel((x, y), color)

def genhqprev(filename, outd):
    with open(filename, 'rb') as fh:
        f = BytesIO(fh.read())
    f.seek(0, 2)
    fsize = f.tell()
    f.seek(0x8) #magic value
    if f.read(7) != 'DSMIO_S' or fsize != 65536:
        return
    
    f.seek(0x100) #background data
    backdata = f.read(0x3000)
    bg = Image.new("RGBA", (192, 128))
    parseimagedata(bg, backdata, 192, 128, 768)
    bg = bg.convert("RGB") #RGB for bg lets us use "paste" easier later
    
    f.seek(0xE5F6)
    objorder = f.read(15)
    
    random.seed(randseed)
    
    objcount = 0
    attachments = [{'obj': -1}]*15
    assets = [{'obj': -1}]*15
    for i in xrange(15): #15 objects
        f.seek(0xB104 + i*136) #offset to object description
        objsize = (ord(f.read(1)) + 1) * 16 #dimensions of the object
        if ord(f.read(1)) == 0: #does object exist?
            continue
        f.read(19)
        artoffs = f.tell()
        f.seek(0xBB89 + 0x30 + i*0x2D0) #offset to "start" ai commands for object
        if ord(f.read(1)) != 0x04: #"start" commands are always a 0x?4(set art) command followed by a 0x?7(placement) command
            continue
        artnum = ord(f.read(1))>>4 #which of the four arts to use for the object
        f.read(10)
        
        doarea = 0
        cmd = bf() #We need a bitfield going forward
        cmd[0:32] = struct.unpack('<I', f.read(4))[0]
        cmd[32:64] = struct.unpack('<I', f.read(4))[0]
        cmd[64:96] = struct.unpack('<I', f.read(4))[0]
        if cmd[0:4] == 7: # 0x?7 placement command
            objx = tosigned(cmd[17:27]) #10 bit signed numbers
            objy = tosigned(cmd[27:37])
            if cmd[4:5] == 1: #attachment to obj relative x,y. Takes priority over "within area". They can sometimes both be set for some reason.
                objx -= 96
                objy -= 64
                obj = cmd[13:17]
                attachments[i] = {'obj': obj, 'xoffs': objx, 'yoffs': objy}
            elif cmd[7:8] == 1: #within area
                #Defer this to later. We need the bounding box of the actual object.
                doarea = 1
        
        f.seek(artoffs + artnum * 28)
        f.read(2)
        frameoffs = ord(f.read(1))
        artim = Image.new("RGBA", (objsize, objsize))
        f.seek(0x3104 + frameoffs * 128)
        framedata = f.read(objsize * objsize // 2)
        parseimagedata(artim, framedata, objsize, objsize, objsize*4)
        
        if doarea == 1: #placing object within area, continued from above
            left = objx
            right = tosigned(cmd[37:47])
            top = objy
            bottom = tosigned(cmd[47:57])
            noovl = cmd[10:11] #"try not to overlap other objects" flag
            
            #new left, top, right, bottom for the object bounds
            tmp = artim.getbbox() #the game itself uses only the space you've filled in on the sprite to determine where to place it
            if tmp:
                nl, nt, nr, nb = tmp
            else: #empty transparent images screw things up
                nl, nt, nr, nb = (0, 0, objsize, objsize)
            nw = nr-nl
            nh = nb-nt
            cdx = nw//2+nl-objsize//2 #center diff x
            cdy = nh//2+nt-objsize//2 #center diff y
            
            #TODO make area selection more accurate
            if right-left <= nw: #area too thin, force center of area width
                left += (right-left)//2+1
                right = left
            if bottom-top <= nh: #area too short, force center of area height
                top += (bottom-top)//2+1
                bottom = top
            if (right-left > nw) and (bottom-top > nh): #area bigger than object size, confine object within boundary
                left += nw//2
                right -= nw//2
                top += nh//2
                bottom -= nh//2
            
            #if noovl == 1:
                #TODO implement "no overlap" flag handling
            #else:
            objx = random.randint(left, right)-cdx
            objy = random.randint(top, bottom)-cdy
        
        objcount += 1
        assets[i] = {'art': artim, 'x': objx, 'y': objy, 'size': objsize}
        
    while 1: #bad and lazy attempt to repeatedly move attached objects to their attachment positions
        moved = 0
        for i in xrange(15):
            att = attachments[i]
            if att['obj'] == -1:
                continue
            obj = assets[i]
            attachedto = assets[att['obj']]
            xoffs = att['xoffs']
            yoffs = att['yoffs']
            attachedx, attachedy = (attachedto['x'], attachedto['y'])
            objx, objy = (obj['x'], obj['y'])
            if (attachedx+xoffs != objx) or (attachedy+yoffs != objy):
                moved = 1
                obj['x'], obj['y'] = (attachedx+xoffs, attachedy+yoffs)
        if moved == 0:
            break
    
    for o in reversed(objorder[:objcount]): #start at the furthest back object and build up
        obj = assets[ord(o)]
        bg.paste(obj['art'], (obj['x']-obj['size']//2, obj['y']-obj['size']//2), obj['art'])
        #we subtract objsize/2 since the object's origin is its center
    
    outfname = os.path.join(outd, os.path.splitext(os.path.basename(filename))[0] + u'.png')
    bg.save(outfname)

def main():
    if not os.path.isdir(u'./mio'):
        print 'No mio folder found!'
        return
    
    d = os.path.join(os.getcwd(), u'hqpreviews') #Make sure output folder exists
    if not os.path.exists(d):
        os.makedirs(d)
    
    i = 1
    for file in glob.glob(u'./mio/*.mio'):
        genhqprev(file, d)
        print '%d       \r' % (i),
        i += 1

if __name__ == '__main__':
    main()
