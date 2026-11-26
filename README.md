# Field Theory（場の理論）

Minecraft **1.21.1 / NeoForge 21.1** 向けの「場」ライブラリ Mod です。アイテムは追加しません。
世界に **他の Mod が読み書きできる共通の「場の層」** を敷くことだけが仕事です。

魔法・技術・環境などの Mod が同じ場を共有すれば、別々の Mod の効果が同じ物理法則のもとで **干渉し合う**
ようになります。最初の利用 Mod は [Field Arcana（場術）](https://github.com/nanaios/field_arcana) です。

---

## 1. 世界モデル

世界のどの点 `x` にも、場の種類ごとに **スカラーポテンシャル φ** と **ベクトル場 E = −∇φ** が定まります。

```
φ(x) = G(x) · ( φ_ambient(x) + Σ_i k · q_i · P(|x − x_i|) )
E(x) = G(x) ·                  Σ_i k · q_i · P′(|x − x_i|) · (x − x_i)/|x − x_i|
```

| 記号 | 意味 | API |
|---|---|---|
| `q_i, x_i` | **源**（点電荷）。永続 or 時限 | `FieldSource` |
| `q_i, x_i` | **放射体**：電荷を持つエンティティ自身も源になる | `FieldWorld.Emitter` |
| `P(r)` | 伝播関数（グリーン関数）。場の種類ごとに固定 | `Propagator` |
| `k` | 結合定数 | `FieldType.strength()` |
| `φ_ambient` | **環境場**（いわば真空期待値）。次元・天候・時刻・高度でデータ駆動 | データパック |
| `G(x)` | **領域**の利得の積。>1 なら収束（レンズ）、0 なら遮蔽 | `FieldRegion` |

- 源は **重ね合わせ** で足し合わされます（双極子・遮蔽・レンズが自然に表現できます）。
- 環境場は一様なので勾配を持たず、**力は生みません**（ゲージの自由度のようなもの）。ポテンシャルの閾値や資源として使われます。

### 物質との結合 —— 相互作用

エンティティは場ごとに **電荷 Q** と **慣性質量 m** を持ち、毎 tick 次の加速度を受けます。

```
a = sign(coupling) · forceScale · (Q / m) · E
```

- `Coupling.LIKE_REPELS`（同符号は反発）／`LIKE_ATTRACTS`（同符号は引力）／`NONE`（力を持たない）
- 電荷 = **固有電荷**（データマップ `base_charges`）＋ **誘起電荷**（実行時に魔法や機械が与える。保存・同期される）
- 固有/誘起いずれかの明示的な電荷を持つエンティティは **放射体** になり、自分の場で他者に作用します（相互作用）。
- 慣性質量の既定値はバウンディングボックスの体積です（データマップで上書き可）。

力以外の作用（ダメージ・束縛・状態変化など）は `FieldInteraction` レジストリに登録した規則として追加できます。
Field Theory 自身は `field_theory:force`（上式の力）だけを持ちます。

### 標準場（四つの基本相互作用）

物理の四つの基本相互作用を下敷きにした 4 種類の場を同梱しています。Mod 間で共有しやすいよう、まずこれらの利用を推奨します。

| ID | 名前 | 伝播関数 | 結合 | 備考 |
|---|---|---|---|---|
| `field_theory:gravis` | 重場 | 1/r（射程 48） | 同符号は引力 | **等価原理**：明示電荷がなければ電荷 = 質量。全ての物体が同じ加速度で落ちる |
| `field_theory:fulgur` | 雷場 | 1/r（射程 32） | 同符号は反発 | 中性の物体は感じない |
| `field_theory:nexus` | 結場 | 湯川型 e^(−r/3)/r（射程 16） | 同符号は引力、強い | 短距離の束縛 |
| `field_theory:mutatio` | 転場 | ガウス型 σ=2（射程 8） | 力なし | 物の「位置」ではなく「正体」を変える場 |

---

## 2. Mod 開発者向けガイド

### 依存関係の追加

現状は Maven Local 経由です。

```bash
# field_theory リポジトリで
./gradlew publishToMavenLocal
```

```groovy
// 利用側 build.gradle
repositories { mavenLocal() }
dependencies { implementation "com.nanaios:field_theory:0.1.0" }
```

`neoforge.mods.toml` に必須依存を宣言してください。

```toml
[[dependencies.your_mod]]
    modId="field_theory"
    type="required"
    versionRange="[0.1.0,)"
    ordering="AFTER"
    side="BOTH"
```

### 場を読む（サーバー・クライアント両方で動作）

```java
FieldWorld world = FieldTheoryAPI.get(level);
FieldSample s = world.sample(FieldTypes.FULGUR.get(), pos);
s.potential();       // φ（環境場込み・利得適用済み）
s.sourcePotential(); // 源だけによる φ
s.vector();          // E
s.intensity();       // |E|
```

### 場を書き換える（論理サーバーのみ。クライアントへは自動同期）

```java
// 10 秒で消える点源
world.putSource(FieldSource.temporary(level, FieldTypes.GRAVIS.get(), pos, 24.0, 200, MY_ORIGIN));

// 固定 ID の永続源（ブロックが所有する源など）。同じ ID で put すると置き換え
world.putSource(FieldSource.permanent(id, FieldTypes.FULGUR.get(), pos, q, MY_ORIGIN));
world.removeSource(id);

// 半径 4 の範囲で全ての場を遮蔽（利得 0）
world.putRegion(FieldRegion.temporary(level, Optional.empty(), center, 4.0, 0.0, 300, MY_ORIGIN));

// エンティティに誘起電荷を与える（15 秒）。そのエンティティは放射体にもなる
Charges.induce(entity, FieldTypes.NEXUS.get(), 3.0, 300);
```

`origin` は「誰が作った源か」を示すタグです。自分の Mod の ID を使うと後片付けや絞り込みが楽になります。

### 独自の場を追加する

```java
public static final DeferredRegister<FieldType> FIELDS =
        DeferredRegister.create(FieldTheoryAPI.FIELD_TYPE_REGISTRY, MOD_ID);

public static final DeferredHolder<FieldType, FieldType> AETHER = FIELDS.register("aether",
        () -> FieldType.builder(Propagator.screened(6.0, 0.5))
                .coupling(Coupling.LIKE_REPELS)
                .range(24.0)
                .forceScale(0.05)
                .color(0xFFE08A)
                .build());
```

翻訳キーは `field_type.<namespace>.<path>` です。

### 独自の相互作用（現象）を追加する

```java
public static final DeferredRegister<FieldInteraction> INTERACTIONS =
        DeferredRegister.create(FieldTheoryAPI.INTERACTION_REGISTRY, MOD_ID);

public static final DeferredHolder<FieldInteraction, FieldInteraction> GLOW =
        INTERACTIONS.register("glow", () -> ctx -> {
            if (!ctx.isServerSide()) return;
            if (ctx.sample(FieldTypes.FULGUR.get()).intensity() > 2.0) { /* ... */ }
        });
```

- 相互作用は毎 tick、サーバーでは全エンティティに、クライアントではローカルプレイヤーにだけ呼ばれます（プレイヤーの移動はクライアントが権威を持つため）。
- 運動は `ctx.accelerate(...)` / `ctx.damp(...)` で要求します。ドライバが運動の権威を持つ側でまとめて適用します（1 tick あたり 0.5 block/tick² で頭打ち）。
- ワールドを変える処理（ダメージ・ブロック変更）は `ctx.isServerSide()` のときだけ行ってください。

### イベント（`NeoForge.EVENT_BUS`）

| イベント | 用途 |
|---|---|
| `FieldSourceEvent.Added`（キャンセル可） | 源の追加を拒否する（反魔法領域・保護 Mod 連携など） |
| `FieldSourceEvent.Removed` | 源の消滅（失効を含む）を知る |
| `FieldForceEvent`（キャンセル可） | エンティティに掛かる加速度を書き換える／無効化する |

### ユーティリティ

- `FieldLines.trace(...)`：力線（E の流線）を RK2 で追跡します。可視化に使えます。
- `FieldLines.fibonacciSphere(n)`：球面上にほぼ均等な n 方向。

---

## 3. データパック

### 環境場 `data/<ns>/field_theory/ambient/*.json`

1 ファイルにオブジェクト 1 つ、または配列。該当する全エントリの和が環境場になります。

```json
{ "field": "field_theory:fulgur", "dimensions": ["minecraft:overworld"],
  "base": 0.2, "rain": 0.8, "thunder": 3.0, "night": 0.0, "min_y": -64, "max_y": 320 }
```

### 固有電荷 `data/<ns>/data_maps/entity_type/base_charges.json`

```json
{ "values": {
    "minecraft:creeper":    { "charges": { "field_theory:fulgur": 2.0 } },
    "minecraft:iron_golem": { "mass": 6.0 } } }
```

同梱の既定値の例：クリーパー・ブレイズは正の雷荷、ガーディアンは負の雷荷、**雷そのもの（lightning_bolt）は雷荷 +20**、
スライムは結荷、シュルカーは負の重荷（周囲をわずかに押し退ける）、アイアンゴーレムやウォーデンは重い、など。

---

## 4. コマンド（権限レベル 2）

```
/fieldtheory sample <field> [pos]
/fieldtheory source add <field> <pos> <charge> [seconds]
/fieldtheory source list
/fieldtheory source clear [field]
/fieldtheory region add <pos> <radius> <gain> [seconds]
/fieldtheory region clear
/fieldtheory charge <targets> <field> <amount> [seconds]
/fieldtheory charge <targets> clear
```

---

## 5. 実装メモ

- **永続化**：源と領域は次元ごとの `data/field_theory_fields.dat` に保存されます。誘起電荷はエンティティのアタッチメントとして保存されます。
- **同期**：ログイン・次元移動・リスポーン時にスナップショット、以後は tick ごとの差分を送信します。誘起電荷は追跡中のプレイヤーへ送ります。
- **空間索引**：源の数が少ないうちは線形走査、32 個を超えると 16×16 のカラム格子で近傍だけを走査します。
- **静止摩擦**：バニラの `LivingEntity` は毎 tick |v| < 0.003 の速度成分をゼロに丸めるため、
  地上の生物はおよそ 0.003 block/tick² 未満の力では動き出しません。弱い遠距離場の効果がわずかに「固着」して見えるのは仕様です。
- 単位系：距離はブロック、時間は tick、加速度は block/tick²（参考：バニラの重力は 0.08）。

---

## 6. 開発

```bash
./gradlew build                 # ビルド
./gradlew runGameTestServer     # ゲーム内テスト（ヘッドレス）
./gradlew runClient             # クライアント起動
./gradlew publishToMavenLocal   # 利用側 Mod から参照できるようにする
```

GameTest（`gametest/FieldTheoryGameTests.java`）では、実サーバー上で次を検証しています。

- 重ね合わせ：等しい電荷の中点で E が打ち消し合い φ が倍になる／双極子では逆になる
- 等価原理による重場の引力、雷場の同符号反発（中性物体は不動）
- 利得 0 の領域による遮蔽
- 電荷を持つエンティティ自身が源となって他者を引く（エンティティ間相互作用）
- 誘起電荷の失効、データパックの環境場、NBT 往復

## ライセンス

未定（現在は `All Rights Reserved`）。公開方針に合わせて `gradle.properties` の `mod_license` を変更してください。
