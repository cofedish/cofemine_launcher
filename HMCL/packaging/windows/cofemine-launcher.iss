; ============================================================================
; CofeMine Launcher — Custom Windows Installer (Inno Setup 6)
; ----------------------------------------------------------------------------
; This script wraps the jpackage-produced app-image into a branded installer.
;
; Inputs (passed by Gradle via `iscc /D...`):
;   AppName          Folder-safe product name (no spaces) e.g. "CofeMine-Launcher"
;   AppMenuName      Human-friendly name shown in UI    e.g. "CofeMine Launcher"
;   AppVersion       SemVer-ish version string          e.g. "1.2.11"
;   AppPublisher     Vendor                             e.g. "cofedish"
;   AppImageDir      Path to jpackage app-image root (containing the .exe)
;   AppIcon          Absolute path to .ico file
;   LicenseFile      Absolute path to LICENSE (shown on License page)
;   OutputDir        Where iscc writes the .exe
;   OutputBase       Basename (without .exe) of the installer
;   WelcomeBanner    (optional) 164×314 BMP shown on Welcome/Finish pages
;   HeaderBanner     (optional) 150×57  BMP shown on inner pages
; ============================================================================

#ifndef AppName
  #error "AppName not defined. Invoke via Gradle packageWindowsInnoSetup task."
#endif
#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif
#ifndef AppMenuName
  #define AppMenuName AppName
#endif
#ifndef AppPublisher
  #define AppPublisher "cofedish"
#endif
#ifndef OutputDir
  #define OutputDir "."
#endif
#ifndef OutputBase
  #define OutputBase (AppName + "-Setup")
#endif

; Stable AppId — never change between releases or upgrades stop working.
; AppId is any unique string; we skip GUID braces to avoid Inno Setup
; misreading them as constant references.
#define MyAppId "cofemine-launcher-cofedish-2026"

[Setup]
AppId={#MyAppId}
AppName={#AppMenuName}
AppVersion={#AppVersion}
AppVerName={#AppMenuName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL=https://github.com/cofedish/cofemine_launcher
AppSupportURL=https://github.com/cofedish/cofemine_launcher/issues
AppUpdatesURL=https://github.com/cofedish/cofemine_launcher/releases

; Default per-user install target: %LocalAppData%\Programs\CofeMine-Launcher.
; `{autopf}` resolves to that under per-user, or to `{pf}` under per-machine.
DefaultDirName={autopf}\{#AppName}
UsePreviousAppDir=yes

; No admin required, but user can opt into per-machine install.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog

; We roll our own shortcuts, so skip the Start-menu-folder page.
DisableProgramGroupPage=yes
DefaultGroupName={#AppMenuName}

; x64 Windows only.
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0

; Modern wizard.
WizardStyle=modern
#ifdef WelcomeBanner
WizardImageFile={#WelcomeBanner}
WizardImageStretch=yes
#endif
#ifdef HeaderBanner
WizardSmallImageFile={#HeaderBanner}
#endif

SetupIconFile={#AppIcon}
UninstallDisplayIcon={app}\{#AppName}.exe,0
UninstallDisplayName={#AppMenuName}
UninstallFilesDir={app}\uninst

OutputDir={#OutputDir}
OutputBaseFilename={#OutputBase}

Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes
InternalCompressLevel=max

LicenseFile={#LicenseFile}

VersionInfoCompany={#AppPublisher}
VersionInfoProductName={#AppMenuName}
VersionInfoProductVersion={#AppVersion}
VersionInfoDescription={#AppMenuName} Setup
VersionInfoVersion={#AppVersion}
VersionInfoCopyright=(C) 2022 huangyuhui; (C) 2026 cofedish

ShowLanguageDialog=auto
AllowCancelDuringInstall=yes
CloseApplications=force
RestartApplications=no
ChangesAssociations=no
ChangesEnvironment=no

[Languages]
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[CustomMessages]
; --- Russian ---------------------------------------------------------------
russian.WelcomeTitle=Добро пожаловать в установщик CofeMine Launcher
russian.WelcomeBody=Мастер установит CofeMine Launcher %1 на ваш компьютер.%n%nCofeMine Launcher — это запускалка Minecraft на базе HMCL, заточенная под сервер CofeMine. С её помощью вы сможете играть в любые версии Minecraft, устанавливать сборки модов и подключаться к серверу одним кликом.%n%nРекомендуется закрыть все остальные приложения перед продолжением.%n%nНажмите «Далее», чтобы продолжить, или «Отмена», чтобы выйти из установщика.
russian.FinishedHeading=Установка завершена — добро пожаловать!
russian.FinishedBody=CofeMine Launcher %1 успешно установлен на ваш компьютер.%n%nЗапустить его можно из меню «Пуск» или с рабочего стола, если вы создали ярлык.
russian.CreateDesktopIcon=Создать ярлык на &рабочем столе
russian.AdditionalIcons=Дополнительные ярлыки:
russian.LaunchAfterInstall=&Запустить CofeMine Launcher

; --- English ---------------------------------------------------------------
english.WelcomeTitle=Welcome to the CofeMine Launcher Setup
english.WelcomeBody=This wizard will install CofeMine Launcher %1 on your computer.%n%nCofeMine Launcher is a custom build of HMCL tailored for the CofeMine server. It lets you play any version of Minecraft, install modpacks and connect to the server with a single click.%n%nIt is recommended that you close all other applications before continuing.%n%nClick Next to continue, or Cancel to exit Setup.
english.FinishedHeading=Installation complete — welcome aboard!
english.FinishedBody=CofeMine Launcher %1 has been installed on your computer.%n%nYou can launch it from the Start menu, or from the desktop shortcut if you chose to create one.
english.CreateDesktopIcon=Create a &desktop shortcut
english.AdditionalIcons=Additional shortcuts:
english.LaunchAfterInstall=&Launch CofeMine Launcher

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Copy the entire jpackage app-image (exe + runtime/ + app/).
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppMenuName}"; Filename: "{app}\{#AppName}.exe"; IconFilename: "{app}\{#AppName}.exe"; Comment: "{#AppMenuName}"
Name: "{autodesktop}\{#AppMenuName}"; Filename: "{app}\{#AppName}.exe"; IconFilename: "{app}\{#AppName}.exe"; Tasks: desktopicon; Comment: "{#AppMenuName}"

[Run]
; Refresh the Windows icon cache so the new CofeMine icon replaces the
; previous HMCL one the explorer had cached for this EXE path. Windows
; 10/11 keeps icons in iconcache_*.db files that don't invalidate just
; because a file was overwritten at the same path. We restart explorer
; (quick flash, no data loss) and then force a shell refresh so taskbar,
; Start-menu and pinned shortcuts all pick up the new icon.
Filename: "{cmd}"; Parameters: "/C taskkill /IM explorer.exe /F & del /a /q ""%localappdata%\IconCache.db"" ""%localappdata%\Microsoft\Windows\Explorer\iconcache_*.db"" 2>nul & start explorer.exe"; Flags: runhidden
Filename: "{sys}\ie4uinit.exe"; Parameters: "-show"; Flags: runhidden
Filename: "{app}\{#AppName}.exe"; Description: "{cm:LaunchAfterInstall}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Remove everything jpackage laid down, plus the uninst folder we created.
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\uninst"
Type: files;         Name: "{app}\{#AppName}.exe"
Type: files;         Name: "{app}\{#AppName}.ico"
Type: dirifempty;    Name: "{app}"

[Code]
function GetUninstallerPath(): String;
var
  subkey, value: String;
begin
  { When a previous install exists Inno Setup stores its uninstaller path
    under HKCU\...\Uninstall\<AppId>_is1. Read it so we can run the old
    uninstaller silently before the new install copies files in. }
  subkey := 'Software\Microsoft\Windows\CurrentVersion\Uninstall\' +
            ExpandConstant('{#MyAppId}') + '_is1';
  Result := '';
  if RegQueryStringValue(HKCU, subkey, 'QuietUninstallString', value) then
    Result := value
  else if RegQueryStringValue(HKCU, subkey, 'UninstallString', value) then
    Result := value
  else if RegQueryStringValue(HKLM, subkey, 'QuietUninstallString', value) then
    Result := value
  else if RegQueryStringValue(HKLM, subkey, 'UninstallString', value) then
    Result := value;
end;

function InitializeSetup(): Boolean;
var
  uninst, execPath, execArgs: String;
  resultCode: Integer;
  spacePos: Integer;
begin
  Result := True;
  uninst := GetUninstallerPath();
  if uninst = '' then
    Exit;

  { Run the previous uninstaller silently so the install looks like an
    in-place upgrade — no confirmation, no duplicate entry in Programs. }
  execPath := uninst;
  execArgs := '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART';

  if (Length(uninst) > 0) and (uninst[1] = '"') then begin
    spacePos := Pos('"', Copy(uninst, 2, Length(uninst)));
    if spacePos > 0 then begin
      execPath := Copy(uninst, 2, spacePos - 1);
      if Length(uninst) > spacePos + 2 then
        execArgs := Copy(uninst, spacePos + 3, Length(uninst)) + ' ' + execArgs;
    end;
  end;

  Exec(execPath, execArgs, '', SW_HIDE, ewWaitUntilTerminated, resultCode);
end;

procedure InitializeWizard();
var
  WelcomeBody, FinishBody: String;
begin
  { Override default captions with our branded multi-paragraph copy.
    Custom messages use %1 as the version placeholder. }
  WelcomeBody := ExpandConstant('{cm:WelcomeBody,{#AppVersion}}');
  FinishBody  := ExpandConstant('{cm:FinishedBody,{#AppVersion}}');

  WizardForm.WelcomeLabel1.Caption        := ExpandConstant('{cm:WelcomeTitle}');
  WizardForm.WelcomeLabel2.Caption        := WelcomeBody;
  WizardForm.FinishedHeadingLabel.Caption := ExpandConstant('{cm:FinishedHeading}');
  WizardForm.FinishedLabel.Caption        := FinishBody;
end;
