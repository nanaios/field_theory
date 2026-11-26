# 変更履歴

## 0.3.0
- 振動する源（`FieldSource.frequency` / `phase`）と遅延伝播：φ = k·q·P(r)·cos(ωt − κr + φ₀)、E は厳密な勾配
- `FieldType.waveSpeed()`（標準場は 0.5 block/tick）
- `FieldWorld.amplitude(type, pos)`：同じ振動数の源をフェーザーとして合成した時間平均振幅（干渉）
- コマンド `/fieldtheory amplitude` と `/fieldtheory source wave`
- ネットワークのプロトコル版を 2 に（源に振動の情報が増えたため）。保存データは旧版と互換
- GameTest を追加（遅延と勾配、同位相の干渉、逆位相の打ち消し）

## 0.2.0
- サーバー設定を追加（力の倍率・最大加速度・プレイヤーへの作用・次元あたりの源の上限）。ゲーム内の設定画面に対応
- 源の数が上限に達すると `putSource` が `false` を返すように
- GameTest を追加（空間格子と総和の一致、源の上限）
- CI：push は main のみ・PR・手動実行で起動。タグ push で GitHub Release に jar を添付

## 0.1.0
- 最初の版：場・源・領域・電荷・相互作用の API、四つの標準場、同期と永続化、`/fieldtheory`
- ライセンスを LGPL-3.0-only に
