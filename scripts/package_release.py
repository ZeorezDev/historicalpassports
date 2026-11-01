"""Package source without local tool caches, worlds, credentials or downloaded source photos."""
from pathlib import Path
import zipfile,hashlib,json
ROOT=Path(__file__).resolve().parents[1];out=ROOT/'dist';out.mkdir(exist_ok=True)
jar=ROOT/'build/libs/historicalpassports-1.1.0.jar'
assert jar.exists(),'Run Gradle build first'
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    assert len([p for p in names if p.startswith('assets/historicalpassports/textures/item/passport_') and p.endswith('.png')])==195
    assert 'ASSET_CREDITS.md' in names and 'META-INF/mods.toml' in names
    header=z.read('historicalpassports/HistoricalPassports.class')[:8]
    assert int.from_bytes(header[6:8],'big')==61,'Must compile for Java 17'
target=out/jar.name;target.write_bytes(jar.read_bytes())
include=['src','gradle','scripts','docs']
files=[]
for folder in include:
    files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
files.extend(p for p in ROOT.iterdir() if p.is_file() and (p.suffix in ['.md','.gradle','.properties'] or p.name in ['LICENSE','LICENSE.txt','CREDITS.txt','gradlew','gradlew.bat','.gitignore','.gitattributes']))
files.extend(p for p in (ROOT/'assets').rglob('*.json') if 'sources' not in p.parts and 'candidates' not in p.parts)
source=out/'historicalpassports-1.1.0-source.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(set(files)):z.write(p,p.relative_to(ROOT).as_posix())
checksums={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [target,source]}
(out/'SHA256SUMS.txt').write_text(''.join(f'{v}  {k}\n' for k,v in checksums.items()))
print(json.dumps({'files':checksums,'source_files':len(set(files))},indent=2))
