# RandomTowerDefense — 태산 팀

Java TCP Socket 기반 실시간 1v1 랜덤 타워 디펜스. **서버 기능별 개발 단계**이며 전체 게임 완성본은 아닙니다.

## 현재 구현된 서버 기능
- 사용자 등록, 다중 방 목록·생성·입장·퇴장, 방장 위임, 준비/취소 및 방장 시작 요청
- 방별 채팅 중계 및 퇴장 시 진행 중인 게임 중단
- 주사위 서버 상태: 15칸 필드, 3종 주사위 랜덤 소환, 같은 종류·등급 합성, 종류별 강화, SP 차감
- 서버 검증 후 방 참가자 둘에게 상태 브로드캐스트

서버는 몬스터·웨이브·전투, 엘리트 소환과 승패를 처리하며, 경기 중 두 플레이어에게 3틱마다 50 SP를 지급합니다. Swing 화면은 대기실·주사위·SP와 양쪽 몬스터를 표시합니다. 승패 화면은 아직 연동되지 않았습니다.

## 개발 환경
- Java 25 사용 계획(팀 합의). 이 배포본의 기본 기능은 OpenJDK 21에서 컴파일·테스트 검증됨.
- TCP, UTF-8, 기본 포트 5000. 외부 Java 라이브러리 불필요.
- Python 3은 자동화된 소켓 통합 테스트를 실행할 때만 필요.

## 실행 (Windows PowerShell, 프로젝트 루트)

서버:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-server.ps1
```

Swing 클라이언트:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-client.ps1
```

자동 테스트(자체 서버를 포트 39871, 39872로 실행함):
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-server.ps1
```

Linux/macOS: `bash scripts/run-server.sh` 또는 `bash scripts/test-server.sh`.

## 팀 협업
- 서버 담당: 김태규. UI/클라이언트 담당: 이산. 게임 규칙, 프로토콜 및 통합 테스트는 공동.
- 기능별 브랜치에서 커밋 후 GitHub에 푸시하고 PR로 검토하여 `main`에 병합.
- 통신 규약 세부사항: [docs/PROTOCOL.md](docs/PROTOCOL.md)
- 서버 구현·테스트 로그 요약: [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md)
