# Instala ou atualiza o OSRS Translate PT-BR (fork) no RuneLite deste PC.
# Rode pelo instalar.bat. Rodar de novo atualiza o plugin e as traducoes.
$ErrorActionPreference = 'Stop'

$repo      = Split-Path -Parent $PSScriptRoot
$runelite  = Join-Path $env:USERPROFILE '.runelite'
$lancador  = Join-Path $env:LOCALAPPDATA 'RuneLite'
$java      = Join-Path $lancador 'jre\bin\javaw.exe'
$bibliotecas = Join-Path $runelite 'repository2'
$plugins   = Join-Path $runelite 'sideloaded-plugins'
$traducoes = Join-Path $runelite 'osrs-translate-ptbr\pt-BR'
$credencial = Join-Path $runelite 'credentials.properties'
$jar       = Join-Path $repo 'dist\osrs-translate-ptbr.jar'

function Passo($texto) { Write-Host ""; Write-Host "== $texto" -ForegroundColor Cyan }

# 1. Confere o RuneLite
Passo 'Conferindo o RuneLite'
if (-not (Test-Path $java) -or -not (Test-Path (Join-Path $bibliotecas 'client-*.jar'))) {
    throw 'RuneLite nao encontrado. Instale o RuneLite, abra o jogo uma vez pelo Jagex Launcher e rode este instalador de novo.'
}
if (-not (Test-Path $jar)) {
    throw "Plugin nao encontrado em $jar. Gere com scripts/gerar-dist.sh."
}
Write-Host 'OK'

# 2. Copia o plugin e as traducoes
Passo 'Copiando o plugin e as traducoes'
New-Item -ItemType Directory -Force -Path $plugins, $traducoes | Out-Null
# Nome usado numa instalacao manual antiga; dois jars carregariam o plugin duas vezes.
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $plugins 'osrs-translate-fork.jar')
Copy-Item -Force $jar (Join-Path $plugins 'osrs-translate-ptbr.jar')
Copy-Item -Force (Join-Path $repo 'traducoes\pt-BR\*.json') $traducoes
$total = (Get-ChildItem $traducoes -Filter *.json).Count
Write-Host "Plugin em $plugins"
Write-Host "$total arquivos de traducao em $traducoes"

# 3. Cria o atalho que abre o RuneLite com o plugin
Passo 'Criando o atalho "RuneLite PT-BR"'
$abrir = Join-Path $runelite 'RuneLite-PTBR.bat'
@(
    '@echo off',
    'rem Abre o RuneLite em modo de desenvolvedor, que carrega o plugin de sideloaded-plugins.',
    'rem O lancador oficial desliga esse modo, por isso o cliente abre direto pelo Java.',
    "start `"`" `"$java`" -ea -Xmx768m -Xss2m -cp `"$bibliotecas\*`" net.runelite.client.RuneLite --developer-mode"
) | Set-Content -Encoding ASCII $abrir

$areaDeTrabalho = [Environment]::GetFolderPath('Desktop')
$atalho = (New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $areaDeTrabalho 'RuneLite PT-BR.lnk'))
$atalho.TargetPath = $abrir
$atalho.WorkingDirectory = $runelite
$atalho.WindowStyle = 7
$icone = Join-Path $lancador 'RuneLite.exe'
if (Test-Path $icone) { $atalho.IconLocation = $icone }
$atalho.Save()
Write-Host "Atalho criado na area de trabalho: RuneLite PT-BR"

# Versao para a Steam: abre o jogo sem janela e so termina quando o jogo fecha,
# entao a Steam conta as horas. Argumentos que a Steam passa sao ignorados.
$steam = Join-Path $runelite 'RuneLite-PTBR-Steam.vbs'
$comando = "`"$java`" -ea -Xmx768m -Xss2m -cp `"$bibliotecas\*`" net.runelite.client.RuneLite --developer-mode"
@(
    "' Abre o RuneLite PT-BR e espera o jogo fechar. Uso na Steam:",
    "' C:\Windows\System32\wscript.exe `"$steam`" %command%",
    "CreateObject(`"WScript.Shell`").Run `"$($comando.Replace('`"', '`"`"'))`", 1, True"
) | Set-Content -Encoding ASCII $steam
Write-Host "Para a Steam: $steam"

# 4. Login com conta Jagex
# O backup do settings.json do lancador fica em disco: se a janela fechar no meio,
# a proxima execucao restaura o arquivo original.
$configuracao = Join-Path $lancador 'settings.json'
$backupConfig = "$configuracao.osrs-ptbr.bak"
$criadoPorNos = "$configuracao.osrs-ptbr.novo"
function Restaurar-Configuracao {
    if (Test-Path $backupConfig) { Move-Item -Force $backupConfig $configuracao }
    elseif (Test-Path $criadoPorNos) {
        Remove-Item -Force -ErrorAction SilentlyContinue $configuracao
        Remove-Item -Force $criadoPorNos
    }
}
Restaurar-Configuracao
Passo 'Login'
if (Test-Path $credencial) {
    Write-Host 'Credencial da conta Jagex ja existe. Nada a fazer.'
} else {
    Write-Host 'Fora do Jagex Launcher, conta Jagex so entra com um arquivo de credencial.'
    Write-Host 'AVISO: esse arquivo guarda o token da conta em texto puro em' -ForegroundColor Yellow
    Write-Host "  $credencial" -ForegroundColor Yellow
    Write-Host 'Quem copiar esse arquivo entra na sua conta. Para cancelar o acesso, apague o arquivo.' -ForegroundColor Yellow
    $resposta = Read-Host 'Sua conta e Jagex e voce quer criar a credencial agora? (s/n)'
    if ($resposta -match '^[sS]') {
        if (Test-Path $configuracao) { Copy-Item -Force $configuracao $backupConfig }
        else { New-Item -ItemType File -Force $criadoPorNos | Out-Null }
        '{ "clientArguments": ["--insecure-write-credentials"] }' | Set-Content -Encoding ASCII $configuracao
        try {
            Write-Host ''
            Write-Host 'Abra o jogo pelo Jagex Launcher agora e espere a tela de login do RuneLite.'
            Write-Host 'Esperando o arquivo de credencial (ate 10 minutos)...'
            $limite = (Get-Date).AddMinutes(10)
            while (-not (Test-Path $credencial) -and (Get-Date) -lt $limite) { Start-Sleep -Seconds 3 }
        } finally {
            # Tira o argumento para o token nao ser regravado a cada abertura.
            Restaurar-Configuracao
        }
        if (Test-Path $credencial) {
            Write-Host 'Credencial criada. Feche o RuneLite aberto pelo Jagex Launcher.' -ForegroundColor Green
        } else {
            Write-Host 'A credencial nao apareceu. Rode o instalador de novo para tentar outra vez.' -ForegroundColor Red
        }
    } else {
        Write-Host 'Sem credencial. Com login antigo (e-mail e senha), entre normalmente pelo atalho.'
    }
}

# 5. Fim
Passo 'Pronto'
Write-Host '1. Se a versao original estiver instalada pelo Plugin Hub, desinstale. As duas entram em conflito.'
Write-Host '2. Abra o jogo pelo atalho "RuneLite PT-BR" na area de trabalho.'
Write-Host '3. O plugin ja vem configurado: traducoes locais e "Traduzir outras interfaces" ligado.'
