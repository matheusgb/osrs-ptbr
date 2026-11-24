# Remove o OSRS Translate PT-BR (fork) deste PC.
# Rode pelo desinstalar.bat, com o jogo fechado. O RuneLite normal nao muda.
$ErrorActionPreference = 'Stop'

$runelite = Join-Path $env:USERPROFILE '.runelite'
$itens = @(
    (Join-Path $runelite 'sideloaded-plugins\osrs-translate-ptbr.jar'),
    (Join-Path $runelite 'sideloaded-plugins\osrs-translate-fork.jar'),
    (Join-Path $runelite 'osrs-translate-ptbr\pt-BR'),
    (Join-Path $runelite 'RuneLite-PTBR.bat'),
    (Join-Path ([Environment]::GetFolderPath('Desktop')) 'RuneLite PT-BR.lnk')
)

# Ler o caminho de um processo de outro usuario pode falhar: esse processo nao conta.
$aberto = Get-Process -Name javaw -ErrorAction SilentlyContinue | Where-Object {
    try { $_.Path -like '*RuneLite*' } catch { $false }
}
if ($aberto) {
    throw 'O RuneLite esta aberto. Feche o jogo e rode o desinstalar.bat de novo.'
}

foreach ($item in $itens) {
    if (Test-Path $item) {
        Remove-Item -Recurse -Force $item
        Write-Host "Removido: $item"
    }
}

# O registro de textos sem traducao fica: ele pode ter textos ainda nao enviados.
$pasta = Join-Path $runelite 'osrs-translate-ptbr'
if ((Test-Path $pasta) -and -not (Get-ChildItem -Force $pasta)) { Remove-Item -Force $pasta }

Write-Host ''
Write-Host 'Pronto. O plugin e o atalho foram removidos.' -ForegroundColor Green
$registro = Join-Path $pasta 'sem-traducao.jsonl'
if (Test-Path $registro) {
    Write-Host "O registro de textos sem traducao ficou em $registro. Apague se nao precisar mais."
}
if (Test-Path (Join-Path $runelite 'credentials.properties')) {
    Write-Host 'A credencial da conta Jagex continua em disco. Para apagar, rode o limpar-credencial.bat.' -ForegroundColor Yellow
}
