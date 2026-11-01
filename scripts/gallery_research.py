"""Use the ordinary-passport gallery as a discovery index; licenses remain per Commons file.
This index does not approve images. It reduces repetitive country search requests.
"""
from commons_assets import *
from bs4 import BeautifulSoup
from urllib.parse import unquote,urlparse
url='https://en.wikipedia.org/wiki/List_of_passports'
r=SESSION.get(url,timeout=45);r.raise_for_status()
html_path=ROOT/'assets/research/passport_gallery.html';html_path.write_text(r.text,encoding='utf-8')
soup=BeautifulSoup(r.text,'html.parser');rows=[]
for box in soup.select('.gallerybox'):
    caption=box.select_one('.gallerytext');image=box.select_one('.thumb a')
    if not caption or not image:continue
    title=unquote(image.get('href','')).split('/wiki/')[-1]
    if title.startswith('./'):title=title[2:]
    img=box.select_one('img')
    rows.append({'caption':caption.get_text(' ',strip=True),'title':title.replace('_',' '),'preview_url':('https:'+img.get('src','')) if img and img.get('src','').startswith('//') else (img.get('src','') if img else '')})
write(ROOT/'assets/gallery_index.json',{'source':url,'entries':rows})
print(f'Gallery indexed: {len(rows)} covers')
for row in rows[:8]: print(row['caption'],row['title'])
