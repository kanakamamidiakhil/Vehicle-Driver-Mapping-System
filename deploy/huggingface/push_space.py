"""Publishes this repository to a Hugging Face Docker Space (created on first run).

Usage: HF_TOKEN=<write token> python deploy/huggingface/push_space.py [--space-name NAME] [--dry-run]
"""
import argparse
import os
import shutil
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
IGNORE = shutil.ignore_patterns(
    "node_modules", "dist", ".angular", "target", ".mvn", "mvnw", "mvnw.cmd", "static", "*.log")


def stage(dest: Path) -> None:
    shutil.copytree(ROOT / "backend", dest / "backend", ignore=IGNORE)
    shutil.copytree(ROOT / "frontend", dest / "frontend", ignore=IGNORE)
    (dest / "deploy" / "huggingface").mkdir(parents=True)
    shutil.copy(HERE / "start.sh", dest / "deploy" / "huggingface" / "start.sh")
    shutil.copy(HERE / "Dockerfile", dest / "Dockerfile")
    shutil.copy(HERE / "SPACE_README.md", dest / "README.md")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--space-name", default=os.environ.get("HF_SPACE_NAME") or "vehicle-driver-mapping")
    parser.add_argument("--dry-run", action="store_true", help="only build the staging folder and list it")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as tmp:
        staging = Path(tmp) / "space"
        stage(staging)
        if args.dry_run:
            for p in sorted(staging.rglob("*")):
                if p.is_file():
                    print(p.relative_to(staging))
            return

        from huggingface_hub import HfApi

        api = HfApi(token=os.environ["HF_TOKEN"])
        user = api.whoami()["name"]
        repo_id = f"{user}/{args.space_name}"
        api.create_repo(repo_id, repo_type="space", space_sdk="docker", exist_ok=True)
        api.upload_folder(
            folder_path=str(staging),
            repo_id=repo_id,
            repo_type="space",
            commit_message="Deploy from GitHub",
            delete_patterns="*",
        )
        print(f"Space page: https://huggingface.co/spaces/{repo_id}")
        print(f"App URL:    https://{user.lower()}-{args.space_name.lower()}.hf.space")


if __name__ == "__main__":
    main()
