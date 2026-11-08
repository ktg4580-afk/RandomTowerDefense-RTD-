# 태산 - Random Tower Defense

Java Socket 기반 2인용 실시간 타워 디펜스 게임 프로젝트 (초기 개발 환경).

> 현재 들어 있는 것은 **2인 접속 / 준비 / 시작 알림 / 간단한 채팅**을 테스트하는 기반 코드입니다. 주사위, 웨이브, 엘리트 몬스터 및 승패 로직은 **아직 구현되지 않았습니다**.

## 요구 환경
- **최신 JDK 설치 및 PATH 등록** (팀원끼리 정확한 버전을 맞출 것; `java -version` / `javac -version` 확인)
- Git
- Windows PowerShell 또는 Linux/macOS 터미널
- 외부 라이브러리 없음 (Java 표준 라이브러리만 사용)
- 기본 서버 포트: `5000` (로컬 접속 `127.0.0.1`)

## Windows 실행
각 명령은 **프로젝트 최상위 폴더**에서 실행합니다.

터미널 1 (서버):
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-server.ps1
```
터미널 2, 3 (클라이언트 2개):
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-client.ps1
```
처음 실행할 때마다 자동 컴파일합니다. IDE에서 `server.GameServer`와 `client.GameClient`를 각각 실행해도 됩니다.

## Linux/macOS 실행
```bash
bash scripts/run-server.sh
# 다른 터미널 두 개에서
bash scripts/run-client.sh
```

## 첫 통합 테스트
1. 서버를 실행하고 2개의 클라이언트를 열어 서로 다른 이름으로 접속합니다.
2. 두 클라이언트에서 각각 **준비 완료**를 누르면 서버가 `/START`를 전송합니다.
3. 한 클라이언트에서 `HELLO`를 입력 후 전송하면 다른 클라이언트에도 채팅이 보입니다.
4. 세 번째 클라이언트 또는 중복 이름 접속은 거절됩니다.
5. 한 클라이언트를 종료하면 다른 클라이언트에 접속 종료 안내가 표시됩니다.

## 구조
```text
src/main/java/
  common/Protocol.java            # 공통 명령 상수
  server/GameServer.java          # 2인용 TCP 서버
  server/ClientHandler.java       # 각 클라이언트 수신 스레드
  client/GameClient.java          # Swing 기반 접속/준비/채팅 테스트 화면
scripts/                        # 컴파일 및 실행
 docs/                           # 프로토콜 · 협업 · 게임 규칙 초안
```

## Git 협업
- `main`: 검증된 코드
- `feature/server`, `feature/client-ui`, `feature/game-logic`: 작업 예시 (필요할 때 브랜치 생성)
- GitHub 원격 저장소는 연결되어 있지 않습니다. README 아래 지침을 따라 **직접 생성 후 연결**하세요.

```bash
git remote add origin https://github.com/OWNER/REPO.git
git push -u origin main
```
위 주소는 예시입니다. GitHub에서 생성한 실제 빈 저장소 주소로 바꾸세요.

## 작업 전 결정 사항
- 팀 공통 JDK의 정확한 버전
- 주사위 및 엘리트 몬스터의 게임 규칙
- 서버에서 사용하는 게임 상태 구조와 전송 주기
- 주차별 담당 역할과 PR 승인 규칙

게임 로직 확장은 `docs/PROTOCOL.md` 및 `docs/GAME_RULES.md`를 먼저 수정한 다음 진행하세요.
