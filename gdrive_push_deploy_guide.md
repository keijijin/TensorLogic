# Google Drive（Desktop同期）上の non-bare リポジトリへ **push して同時に作業ツリー更新**する「正解筋」ガイド

このガイドは、**同一マシン**上のローカル作業リポジトリから、Google Drive for Desktop で同期中の **non-bare Git リポジトリ**（作業ツリーあり）へ直接 `git push` し、**push と同時に Google Drive 側の作業ツリーも更新**するまでの正解ルート（実証済み手順）をまとめたものです。

> ポイント：
> - **SSH 経由**で push する（`git-receive-pack` を必ず通す）
> - 受け側は **チェックアウト中のブランチを持たない（detached HEAD）** 状態にする
> - 受け側のサーバフック（`post-receive`）で **作業ツリーを最新コミットへ強制反映**する

---

## 前提条件

- macOS で Google Drive for Desktop により、`/Users/<you>/Library/CloudStorage/GoogleDrive-<account>/マイドライブ/repos/TensorLogic` が同期されている
- **受け側（Google Drive 側）**には事前に `git init` 済み（non-bare）で、`main` ブランチが存在
- **送り側（ローカル作業）**は `/Users/<you>/ai/TensorLogic` に存在し、`main` の初回コミットが作成済み
- Git と SSH が利用可能（macOS の「リモートログイン」を有効にする）

---

## ステップ 1：受け側（GDrive 側）準備

1. **post-receive フック**を設置（push 後に作業ツリーを最新へ強制更新）  
   ```bash
   cd "/Users/kjin/Library/CloudStorage/GoogleDrive-kjin@redhat.com/マイドライブ/repos/TensorLogic"
   # チェックアウト中でも push を受け付ける（フックで整合させるため）
   git config receive.denyCurrentBranch ignore
   
   cat > .git/hooks/post-receive <<'EOF'
   #!/usr/bin/env bash
   set -e
   while read oldrev newrev refname; do
     # main ブランチに push されたときだけ反映
     if [ "$refname" = "refs/heads/main" ]; then
       # non-bare リポジトリ: カレント直下がワークツリー
       git reset --hard "$newrev"
     fi
   done
   EOF
   chmod +x .git/hooks/post-receive
   ```

2. **受け側を detached HEAD** にして、サーバ側の「チェックアウト中ブランチ更新拒否」条件を外す  
   ```bash
   # いま main がチェックアウトされている想定
   git symbolic-ref -q --short HEAD   # => main
   # detached に切り替え
   git checkout --detach
   ```

> これで、push 時に **post-receive フックが `git reset --hard <newrev>` を実行**し、Drive 側の作業ツリーが最新コミットに更新されます。

---

## ステップ 2：送り側（ローカル作業）準備

1. **remote を SSH** にする（同一マシンでも SSH を使うのが確実）  
   ```bash
   cd /Users/kjin/ai/TensorLogic
   git remote set-url gdrive "ssh://localhost/Users/kjin/Library/CloudStorage/GoogleDrive-kjin@redhat.com/マイドライブ/repos/TensorLogic"
   ```

2. 初回接続時のホスト鍵確認に応答し、必要ならパスワード入力または鍵認証を設定する。

---

## ステップ 3：push 実行

```bash
cd /Users/kjin/ai/TensorLogic
git push -u gdrive main
```

- 正常終了の一例：
  ```text
  remote: HEAD is now at b382e10 initial commit
  To ssh://localhost/Users/.../マイドライブ/repos/TensorLogic
     c9d0393..b382e10  main -> main
  branch 'main' set up to track 'gdrive/main'.
  ```
- このメッセージが出れば、**Drive 側で post-receive が発火し、ワークツリーが更新**されています。

---

## 検証（受け側）

```bash
cd "/Users/kjin/Library/CloudStorage/GoogleDrive-kjin@redhat.com/マイドライブ/repos/TensorLogic"
git status                # -> working tree clean
git rev-parse HEAD        # -> 送り側の最新コミットハッシュと一致
```

Finder/Google Drive フォルダを見ても、ファイル群が更新されているはずです。

---

## 運用上の注意・Tips

- **Google Drive 同期と Git の相性**：`.git` 配下や大量小ファイルの変更が連続するため、並行編集・同時 push は避ける。必要なら同期一時停止→push→再開。
- **受け側での直接編集はしない**：post-receive が `reset --hard` を行うため、受け側で未コミット変更があると上書きされる。
- **SSH 認証**：パスワード入力が手間な場合は、`~/.ssh/authorized_keys` に公開鍵を登録して鍵認証へ移行。
- **もっと堅牢に**：
  - Drive 側を **bare リポジトリ**（`TensorLogic.git`）にし、`post-receive` で **Drive の外**の展開先ワークツリーへ `checkout -f`/`reset --hard` する構成が安全。
  - あるいは GitHub/GitLab を **中継（origin）**にし、ローカルは **ミラー（mirror）** として Drive 展開用に push する二段構えも有効。

---

## トラブルシューティング早見表

| 症状 | 原因 | 解決策 |
| --- | --- | --- |
| `remote: error: refusing to update checked out branch` | 受け側がブランチをチェックアウト中 | **受け側を detached HEAD** にする／bare にする |
| `updateInstead` でも拒否 | ファイルプロトコル経由や環境差で受信側処理が動かない | **SSH 経由**で push／フック設置 |
| push 成功するがワークツリーが更新されない | フック未設置・権限なし | `.git/hooks/post-receive` が **実行可能**（`chmod +x`）か確認 |
| パスが微妙に違う（濁点の異体字） | 実は **別ディレクトリ** | `pwd` をコピペして remote URL を **正確に**設定 |
| 毎回パスワードを求められる | 鍵認証未設定 | `ssh-keygen`→公開鍵を `~/.ssh/authorized_keys` へ配置 |

---

## 参考コマンド（抜粋）

```bash
# 受け側：設定確認
git -C "/Users/kjin/Library/CloudStorage/.../TensorLogic" config -l | grep receive
ls -l "/Users/kjin/Library/CloudStorage/.../TensorLogic/.git/hooks/post-receive"

# 送り側：remote 確認
git remote -v
```

---

以上で、**Google Drive 同期フォルダ内の non-bare リポジトリに push → 自動で作業ツリーを更新**するまでの正解ルートのガイドは完了です。
