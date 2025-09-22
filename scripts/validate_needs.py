#!/usr/bin/env python3
"""Validation script for sphinx-needs JSON exports from Visual Paradigm plugin.

This script validates the exported JSON file against the official sphinx-needs schema to
ensure it can be successfully imported by sphinx-needs.

"""

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any, Dict

try:
    import jsonschema
except ImportError:
    print(
        '❌ Error: jsonschema library is required. Install with: pip install jsonschema'
    )
    sys.exit(1)


class NeedsValidator:
    """Validates sphinx-needs JSON files using official schema."""

    def __init__(self, schema_file: Path = None):
        self.errors = []
        self.warnings = []
        self.schema = self._load_schema(schema_file)

    def _load_schema(self, schema_file: Path = None) -> Dict[str, Any]:
        """Load the sphinx-needs JSON schema."""
        if schema_file is None:
            # Use bundled schema
            script_dir = Path(__file__).parent
            schema_file = script_dir.parent / 'schemas' / 'sphinx-needs-5.1.0-schema.json'

        try:
            with open(schema_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            self.errors.append(f"Schema file not found: {schema_file}")
            return {}
        except json.JSONDecodeError as e:
            self.errors.append(f"Invalid schema JSON: {e}")
            return {}

    def validate_file(self, json_file: Path) -> bool:
        """Validate a needs JSON file against the schema."""
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except FileNotFoundError:
            self.errors.append(f"File not found: {json_file}")
            return False
        except json.JSONDecodeError as e:
            self.errors.append(f"Invalid JSON: {e}")
            return False

        return self._validate_with_schema(
            data) and self._validate_additional_rules(data)

    def _validate_with_schema(self, data: Dict[str, Any]) -> bool:
        """Validate data against the official sphinx-needs schema."""
        if not self.schema:
            self.errors.append('No schema available for validation')
            return False

        try:
            jsonschema.validate(instance=data, schema=self.schema)
            return True
        except jsonschema.ValidationError as e:
            self.errors.append(f"Schema validation error: {e.message}")
            if e.path:
                path = ' -> '.join(str(p) for p in e.path)
                self.errors.append(f"  At path: {path}")
            return False
        except jsonschema.SchemaError as e:
            self.errors.append(f"Invalid schema: {e.message}")
            return False

    def _validate_additional_rules(self, data: Dict[str, Any]) -> bool:
        """Validate additional business rules not covered by schema."""
        success = True

        # Check versions structure exists
        if 'versions' not in data:
            return success

        for version, version_data in data['versions'].items():
            if 'needs' not in version_data:
                continue

            # Check needs_amount consistency
            actual_count = len(version_data['needs'])
            declared_count = version_data.get('needs_amount', 0)
            if actual_count != declared_count:
                self.warnings.append(
                    f"Version '{version}': needs_amount ({declared_count}) "
                    f"doesn't match actual count ({actual_count})")

            # Validate link integrity
            self._validate_link_integrity(version, version_data['needs'])

            # Validate individual needs
            for need_id, need_data in version_data['needs'].items():
                self._validate_need_rules(need_id, need_data)

        return success

    def _validate_need_rules(self, need_id: str, need_data: Dict[str,
                                                                 Any]) -> None:
        """Validate additional rules for individual needs."""
        # Check ID consistency
        if 'id' in need_data and need_data['id'] != need_id:
            self.errors.append(
                f"Need ID mismatch: key='{need_id}', id='{need_data['id']}'")

        # Validate ID format
        if 'id' in need_data:
            self._validate_id_format(need_data['id'])


    def _validate_link_integrity(self, version: str, needs: Dict[str, Any]) -> None:
        """Validate that all links point to existing needs."""
        valid_ids = set(needs.keys())

        for need_id, need_data in needs.items():
            # Check all relationship types
            for relationship_type in ['includes', 'extends', 'associates', 'links']:
                targets = need_data.get(relationship_type, [])
                for target_id in targets:
                    if target_id not in valid_ids:
                        self.errors.append(
                            f"Need '{need_id}' {relationship_type} non-existent need '{target_id}'")

    def _validate_id_format(self, need_id: str) -> None:
        """Validate need ID format."""
        if not re.match(r'^[A-Z0-9_]+$', need_id):
            self.warnings.append(
                f"Need ID '{need_id}' should contain only uppercase letters, "
                'numbers, and underscores')

    def print_results(self) -> None:
        """Print validation results."""
        if self.errors:
            print('❌ VALIDATION ERRORS:')
            for error in self.errors:
                print(f"  - {error}")
            print()

        if self.warnings:
            print('⚠️  VALIDATION WARNINGS:')
            for warning in self.warnings:
                print(f"  - {warning}")
            print()

        if not self.errors and not self.warnings:
            print('✅ Validation passed! The needs file is valid.')
        elif not self.errors:
            print('✅ Validation passed with warnings.')
        else:
            print('❌ Validation failed with errors.')


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Validate sphinx-needs JSON files against official schema')
    parser.add_argument('json_file',
                        type=Path,
                        help='Path to the needs JSON file to validate')
    parser.add_argument(
        '--schema',
        type=Path,
        help='Path to custom schema file (default: bundled sphinx-needs schema)'
    )
    parser.add_argument('--strict',
                        action='store_true',
                        help='Treat warnings as errors')

    args = parser.parse_args()

    if not args.json_file.exists():
        print(f"❌ File not found: {args.json_file}")
        sys.exit(1)

    if args.schema and not args.schema.exists():
        print(f"❌ Schema file not found: {args.schema}")
        sys.exit(1)

    validator = NeedsValidator(schema_file=args.schema)
    is_valid = validator.validate_file(args.json_file)

    validator.print_results()

    # Exit with error code if validation failed
    if not is_valid or (args.strict and validator.warnings):
        sys.exit(1)

    sys.exit(0)


if __name__ == '__main__':
    main()
