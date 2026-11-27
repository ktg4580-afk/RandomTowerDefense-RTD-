# 통신 규약 v0.2 — 방 기반 대기실

서버 통신: TCP, UTF-8, `\n`으로 구분되는 한 줄당 하나의 명령. 첫 명령은 `/JOIN 닉네임`이며, 이는 **서버 사용자 등록**이지 방 자동 입장이 아닙니다. `/JOIN_OK` 뒤에 방을 만들거나 선택합니다.

닉네임 및 방 이름은 1~20자(문자·숫자·`_`·`-`), 방 이름에는 공백·콜론을 포함할 수 없습니다. 방의 최대 인원은 2명이며, 방을 만든 사람이 방장입니다. **방장을 포함한 두 명 모두 `/READY`를 보내야 방장이 `/START_REQUEST`로 게임 시작을 요청할 수 있습니다.**

| 기능 | 전송 방향 | 명령 예시 | 의미 |
|---|---|---|---|
| 사용자 등록 | C→S | `/JOIN Player1` | 서버 닉네임 등록 |
| 사용자 등록 성공 | S→C | `/JOIN_OK 1` | 서버 내 고유 플레이어 ID (전역 ID) |
| 방 목록 요청 | C→S | `/ROOM_LIST` | 방 목록 조회 |
| 방 목록 응답 | S→C | `/ROOM_LIST_RESULT 1:RoomA:1/2:WAITING 2:RoomB:2/2:PLAYING` | 각각 `방ID:방이름:인원:상태`, 항목은 공백 구분. 방이 없으면 `/ROOM_LIST_RESULT`만 전송 |
| 방 생성 | C→S | `/ROOM_CREATE RoomA` | 방 생성 및 생성자 입장 |
| 방 생성 성공 | S→C | `/ROOM_CREATED 1` | 생성된 방 ID |
| 방 입장 | C→S | `/ROOM_JOIN 1` | 지정한 방 입장 |
| 방 입장 성공 | S→C | `/ROOM_JOINED 1` | 참가한 방 ID |
| 방 상태 | S→방원 | `/ROOM_STATE 1 1 WAITING 1:READY 2:NOT_READY` | `방ID 방장ID 상태 플레이어ID:준비상태 ...` |
| 준비/준비 취소 | C→S | `/READY`, `/UNREADY` | 본인 준비 상태 변경 |
| 방장 시작 요청 | C→S | `/START_REQUEST` | 서버가 방장·정원·READY 여부 검증 |
| 시작 승인 | S→방원 | `/START 1` | 방 ID 포함. 현재는 시작 알림까지만 구현됨 |
| 채팅 | C→S | `/CHAT HELLO` | 같은 방에만 전송 |
| 채팅 전달 | S→방원 | `/CHAT 1 HELLO` | 발신자 플레이어 ID 포함 |
| 방 나가기 | C→S | `/ROOM_LEAVE` | 현재 방에서만 퇴장 (서버 연결 유지) |
| 나가기 완료 | S→C | `/ROOM_LEFT 1` | 방 ID 포함 |
| 다른 유저 퇴장 | S→방원 | `/PLAYER_LEFT 2` | 퇴장 플레이어 ID |
| 오류 | S→C | `/ERROR PLAYERS_NOT_READY` | 조건 불충족 |

방장 퇴장 시 남은 참가자가 방장이 되며, 방에 사람이 없으면 방이 삭제됩니다. **진행 중인 게임에서 퇴장하면** 남은 플레이어에게 `/ERROR GAME_INTERRUPTED`를 전송하고 방을 대기 상태로 돌려 READY를 초기화합니다.

## 주요 오류 코드
`NOT_IN_ROOM`, `ALREADY_IN_ROOM`, `ROOM_FULL`, `ROOM_NOT_FOUND`, `INVALID_ROOM_ID`, `INVALID_ROOM_NAME`, `NOT_HOST`, `PLAYERS_NOT_READY`, `ALREADY_READY`, `ALREADY_NOT_READY`, `GAME_ALREADY_STARTED`, `CHAT_TOO_LONG`, `UNKNOWN_COMMAND`. 등록 실패는 `/JOIN_FAIL INVALID_NAME`, `/JOIN_FAIL DUPLICATE_NAME`.

## UI 연동 주의
`client/GameClient.java`는 **이전 단계의 임시 네트워크 테스트 UI**이며 방 생성·입장 버튼이 아직 없습니다. 서버의 `/JOIN` 다음에 자동으로 `/WAIT`를 받거나 `/READY`만 눌러 시작되는 옛 방식은 더 이상 지원하지 않습니다. UI 구현 담당자가 이 프로토콜에 맞게 화면을 연결해야 합니다.

## 이후 구현 예정 (아직 미구현)
`/SUMMON`, `/MERGE`, `/ELITE_SUMMON`, `/STATE`, `/WAVE`, `/HP`, `/GAME_OVER`: 필드 규격·게임 규칙을 공동으로 결정한 다음 서버 검증 로직을 추가할 예정입니다.

## 1주차 추가 — 주사위/자원 서버 규약 v0.3 (서버 구현 완료, UI 연동 대기)

`/START <방ID>` 다음에 서버는 해당 방원 두 명 모두에게 `/DICE_STATE` 2줄(각 플레이어 1줄씩)을 전송합니다. 상태가 바뀔 때도 두 줄을 전송합니다. 데이터는 서버가 결정하며 클라이언트는 자신의 SP/필드 값을 직접 확정하여 보내지 않습니다.

| 요청 | 방향 | 예시 | 처리 |
|---|---|---|---|
| 소환 | C→S | `/SUMMON` | 보유 SP와 빈칸을 확인해 랜덤 빈칸에 랜덤 1레벨 주사위 생성 |
| 합성 | C→S | `/MERGE 0 4` | 같은 종류·같은 레벨의 두 칸 합성. `0`은 사라지고 `4`에 같은 종류의 상위 레벨 주사위 생성 |
| 강화 | C→S | `/UPGRADE BASIC` | BASIC 종류 전체의 강화 단계 증가. 주사위 개별 등급과 별개 |
| 상태 | S→같은 방 전체 | `/DICE_STATE 1 90 15 0 0 0 BASIC:1 - ...` | `플레이어ID SP 다음소환비용 BASIC강화 RAPID강화 SLOW강화 칸0 ... 칸14` |
| 오류 | S→요청자 | `/ERROR NOT_ENOUGH_SP` | 자원 부족 등. 실패 시 상태는 변하지 않음 |

* 필드: 3×5, **인덱스 0~14**(좌상단부터 행 우선). 빈 칸은 `-`, 차 있는 칸은 `BASIC:1`, `RAPID:2`와 같은 `종류:레벨` 형식.
* 시작 SP: 플레이어당 100. 소환 비용: 10, 15, 20, ... (소환 성공 횟수 기준). **초기 테스트용 수치**이며 팀 합의로 조정 가능.
* 주사위 종류: `BASIC`, `RAPID`, `SLOW`. 합성은 같은 종류/등급만 가능하며 최고 등급은 5.
* 강화 비용: 종류별 첫 강화 50SP, 두 번째 강화 100SP, ...; 강화 단계는 0부터 시작하고 최대 4. **초기 테스트용 수치**.
* 이 버전에는 **몬스터 처치 보상, 전투 계산, 웨이브, 엘리트 소환이 없음**. 테스트 중 SP는 소환/강화로만 감소함.
* 상태/오류 값은 공백으로 구분, 모든 응답은 UTF-8 `println` 한 줄. 타입은 공백 없는 대문자 문자열.
* 오류: `GAME_NOT_STARTED`, `NOT_IN_ROOM`, `FIELD_FULL`, `NOT_ENOUGH_SP`, `INVALID_ARGUMENTS`, `INVALID_CELL`, `EMPTY_CELL`, `MERGE_MISMATCH`, `MAX_LEVEL`, `INVALID_DICE_TYPE`, `MAX_UPGRADE`. 실패 시 요청자에게만 전송.
* 게임 중 상대가 퇴장하면 현재 게임 및 주사위 상태를 폐기하고 대기실로 되돌림.
* 서버는 각 방의 `GameServer.lock` 보호 하에 순차적으로 요청을 처리함. 방 사이 필드는 독립적임. 화면은 UI 담당자가 위 형식에 맞춰 연동해야 함.

## 전투 표시 동기화

서버는 몬스터 상태와 주사위 상태 뒤에 같은 방의 두 클라이언트에게
`/ATTACK <플레이어ID> <발사틱> <몬스터ID> <발사칸비트마스크>`를 전송합니다.
비트 0~14는 주사위 칸 인덱스에 대응합니다. 서버는 발사 시점의 피해를 저장하고
다음 틱에서 해당 몬스터에 적용합니다. 클라이언트는 공격 알림으로 총알을 표시하고,
다음 몬스터 상태를 받으면 피격 효과와 체력 변화를 함께 표시합니다.
