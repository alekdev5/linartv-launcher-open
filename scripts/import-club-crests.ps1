param([string]$SourceRoot = (Join-Path (Split-Path -Parent $PSScriptRoot) '..\club-crests'))
$ErrorActionPreference = 'Stop'
$assets = Join-Path (Split-Path -Parent $PSScriptRoot) 'app\src\main\assets'
$manifestPath = Join-Path $assets 'crests.json'
$manifest = [Collections.Generic.List[object]]::new()
$manifestText = Get-Content -LiteralPath $manifestPath -Encoding UTF8 -Raw
foreach ($entry in ($manifestText | ConvertFrom-Json)) { $manifest.Add($entry) }
$packs = @{
    'i_liga' = @{ Folder='i-liga'; Clubs=[ordered]@{
        'Arka Gdynia'='arka-gdynia.png'
        'Bruk-Bet Termalica Nieciecza'='bruk-bet-termalica.png'
        'Chrobry Głogów'='chrobry-glogow.png'
        'Lechia Gdańsk'='lechia-gdansk.png'
        'Miedź Legnica'='miedz-legnica.png'
        'Odra Opole'='odra-opole.png'
        'Podbeskidzie Bielsko-Biała'='ts-podbeskidzie.png'
        'Pogoń Grodzisk Mazowiecki'='pogon-grodzisk.png'
        'Pogoń Siedlce'='pogon-siedlce.png'
        'Polonia Bytom'='polonia-bytom.png'
        'Polonia Warszawa'='polonia-warszawa.png'
        'Puszcza Niepołomice'='puszcza_niepolomice.png'
        'Ruch Chorzów'='ruch-chorzow.png'
        'Stal Mielec'='stal-mielec.png'
        'Stal Rzeszów'='stal-rzeszow.png'
        'Unia Skierniewice'='unia-skierniewice.png'
        'Warta Poznań'='warta_poznan.png'
        'ŁKS Łódź'='lks-lodz.png'
    }}
    'ii_liga' = @{ Folder='ii-liga'; Clubs=[ordered]@{
        'Avia Świdnik'='avia-swidnik.png'
        'Chojniczanka Chojnice'='chojniczanka.png'
        'GKS Tychy'='gks-tychy.png'
        'Górnik Łęczna'='gornik-leczna.png'
        'Hutnik Kraków'='hutnik-krakow.png'
        'Lechia Zielona Góra'='lechia-zielona-gora.png'
        'Legia II Warszawa'='legia-ii-warszawa.png'
        'Sandecja Nowy Sącz'='mks-sandecja.png'
        'Znicz Pruszków'='mks-znicz-pruszkow.png'
        'Podhale Nowy Targ'='npk-podhale.png'
        'Olimpia Grudziądz'='olimpia-grudziadz.png'
        'Rekord Bielsko-Biała'='rekord-bielsko.png'
        'Resovia'='resovia.png'
        'Śląsk II Wrocław'='slask-wroclaw.png'
        'Sokół Kleczew'='sokol-kleczew.png'
        'Stal Stalowa Wola'='stal-stalowa-wola.png'
        'Świt Szczecin'='swit-szczecin.png'
        'Zawisza Bydgoszcz'='zawisza-bydgoszcz.png'
    }}
    'iii_liga' = @{ Folder='iii-liga-gr-iv'; Clubs=[ordered]@{
        'AKS 1947 Busko Zdrój'='AKSBusko.png'
        'Chełmianka Chełm'='ChelmiankaChelm.png'
        'Czarni Połaniec'='CzarniPolaniec.png'
        'Hetman Zamość'='HetmanZamosc.png'
        'JKS Jarosław'='JKSJaroslaw.png'
        'KSZO 1929 Ostrowiec Świętokrzyski'='KSZO1929.png'
        'Korona II Kielce'='KoronaKielce.png'
        'Moravia Morawica'='MoraviaMorawica.png'
        'Naprzód Jędrzejów'='NaprzodJedrzejow.png'
        'Podlasie Biała Podlaska'='PodlasieBialaPodlaska.png'
        'Pogoń-Sokół Lubaczów'='PogonSokolLubaczow.png'
        'Siarka Tarnobrzeg'='SiarkaTarnobrzeg.png'
        'Sokół Kolbuszowa Dolna'='SokolKD.png'
        'Star Starachowice'='StarStarachowice.png'
        'Wieczysta II Kraków'='WieczystaKrakow.png'
        'Wisła II Kraków'='WislaKrakow.png'
        'Wisłoka Dębica'='WislokaDebica.png'
        'Wiślanie Skawina'='wislanieSkawina.png'
    }}
}
$packs['3-liga-grupa-2'] = @{ Folder='iii-liga-gr-ii'; Clubs=[ordered]@{
    'Polonia Środa Wielkopolska'='polonia-sroda-wielkopolska.png'
}}
# Resolve every club association before copying anything.
$transfers = foreach ($league in $packs.Keys) {
    $pack = $packs[$league]
    $files = @(Get-ChildItem -LiteralPath (Join-Path $SourceRoot $pack.Folder) -File | Where-Object {
        $_.Extension -eq '.png' -or ($_.Extension -eq '.webp' -and -not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($_.FullName, '.png'))))
    })
    if ($files.Count -ne $pack.Clubs.Count) { throw "Unexpected image count for $league" }
    foreach ($club in $pack.Clubs.Keys) {
        $selected = @($files | Where-Object { $_.Name -like $pack.Clubs[$club] })
        if ($selected.Count -ne 1) { throw "Ambiguous or missing crest: $club" }
        $existing = $manifest | Where-Object { $_.league -eq $league -and $_.name -eq $club } | Select-Object -First 1
        $stem = $club.ToLowerInvariant().Replace('ł','l').Normalize([Text.NormalizationForm]::FormD) -replace '\p{M}', '' -replace '[^a-z0-9]+','_'
        $extension = $selected[0].Extension.ToLowerInvariant()
        $asset = if ($existing -and $existing.asset -like 'crests/curated/*') { [IO.Path]::ChangeExtension($existing.asset, $extension) } else { "crests/curated/${league}_${stem}${extension}" }
        [pscustomobject]@{League=$league;Club=$club;File=$selected[0];Entry=$existing;Asset=$asset}
    }
}
if (@($transfers | Group-Object { $_.File.FullName } | Where-Object Count -ne 1).Count) { throw 'Duplicate source association' }
New-Item -ItemType Directory -Path (Join-Path $assets 'crests\curated') -Force | Out-Null
foreach ($transfer in $transfers) {
    $destination = Join-Path $assets $transfer.Asset
    Copy-Item -LiteralPath $transfer.File.FullName -Destination $destination
    if ((Get-FileHash -LiteralPath $destination).Hash -ne (Get-FileHash -LiteralPath $transfer.File.FullName).Hash) { throw 'Crest copy mismatch' }
    if ($transfer.Entry) { $transfer.Entry.asset=$transfer.Asset; $transfer.Entry.source='curated' }
    else { $manifest.Add([pscustomobject]@{name=$transfer.Club;league=$transfer.League;source='curated';teamId=0;url='';asset=$transfer.Asset}) }
}
$polonia = $manifest | Where-Object { $_.name -eq 'Polonia Środa Wielkopolska' } | Select-Object -First 1
$polonia.teamId = 88
$slaskSource = Join-Path $SourceRoot 'ekstraklasa\SlaskWroclaw.png'
if (Test-Path -LiteralPath $slaskSource) {
    $slask = $manifest | Where-Object { $_.league -eq 'ekstraklasa' -and $_.name -eq 'Śląsk Wrocław' } | Select-Object -First 1
    if ($slask) {
        $slask.asset = 'crests/curated/ekstra_slaskwroclaw.png'
        $slask.source = 'curated'
        Copy-Item -LiteralPath $slaskSource -Destination (Join-Path $assets $slask.asset)
        if ((Get-FileHash -LiteralPath $slaskSource).Hash -ne (Get-FileHash -LiteralPath (Join-Path $assets $slask.asset)).Hash) { throw 'Slask copy mismatch' }
    }
}
[IO.File]::WriteAllText($manifestPath, (ConvertTo-Json -InputObject @($manifest.ToArray()) -Depth 8) + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Imported and hash-verified $($transfers.Count) crests, including Polonia Sroda Wielkopolska."
