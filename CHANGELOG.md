# 変更履歴

## 0.4.0
- 誘電分極（誘電泳動）：分極率 α を持つ中性の物体が ∇|E|² に沿って、場の符号によらず強い方へ引かれる。組み込みの相互作用 `field_theory:polarization`
- `base_charges` に `polarizability`、アイテム用データマップ `field_theory:polarizability` を追加（既定：金属類、アイアンゴーレム、トロッコ）
- 領域の空間索引（カラム格子）。領域を多数置いても利得の計算が重くならない
- GameTest を追加（領域の索引と総当たりの一致、誘電泳動）。各テストの開始時に前のテストの残りを片付けるようにした

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
