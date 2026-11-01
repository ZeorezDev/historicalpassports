"""Produce review-only contact sheets from downloaded cover candidates."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
files=sorted((ROOT/'assets/gallery_previews').glob('*.png'))
for start in range(0,len(files),20):
    sheet=Image.new('RGB',(5*170,4*245),'#d0d0d0');d=ImageDraw.Draw(sheet)
    for i,path in enumerate(files[start:start+20]):
        im=Image.open(path).convert('RGBA');im.thumbnail((160,220));x=i%5*170;y=i//5*245
        sheet.paste(im,(x,y+20),im);d.text((x+4,y+3),path.stem,fill='black')
    sheet.save(ROOT/f'assets/gallery-review-{start//20}.png')
print(f'{len(files)} covers / {(len(files)+19)//20} contact sheets')
