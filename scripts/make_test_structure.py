"""Write a minimal valid 4x4x4 empty Minecraft structure without external NBT packages."""
from pathlib import Path
import gzip,struct
def s(v):
    b=v.encode();return struct.pack('>H',len(b))+b
def named(t,n,payload):return bytes([t])+s(n)+payload
def integer(v):return struct.pack('>i',v)
def lst(t,values):return bytes([t])+integer(len(values))+b''.join(values)
root=named(10,'',named(3,'DataVersion',integer(3465))+named(9,'size',lst(3,[integer(4)]*3))+named(9,'palette',lst(10,[named(8,'Name',s('minecraft:air'))+b'\0']))+named(9,'blocks',lst(10,[]))+named(9,'entities',lst(10,[]))+b'\0')
path=Path(__file__).resolve().parents[1]/'src/main/resources/data/historicalpassports/structures/empty.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(root,mtime=0))
