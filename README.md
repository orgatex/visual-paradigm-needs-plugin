# Visual Paradigm Sphinx-Needs Plugin

A Visual Paradigm plugin that exports use case diagrams to Sphinx-Needs compatible JSON format,
enabling seamless integration between Visual Paradigm diagrams and Sphinx documentation systems.

## Architecture

- **Visual Paradigm Plugin API**: Core integration with Visual Paradigm
- **Jackson JSON**: JSON serialization/deserialization
- **Sphinx-Needs Format**: Compatible with sphinx-needs needsfile schema
- **Java Swing**: User interface components

## Features

### Export Capabilities

- **Diagram Export**: Extract use case diagrams to JSON format
- **Actor Mapping**: Convert actors to sphinx-needs actor type
- **Use Case Mapping**: Convert use cases to sphinx-needs req type
- **Relationship Preservation**: Maintain links between diagram elements
- **Metadata Retention**: Preserve Visual Paradigm source IDs and element types

### JSON Format

- **Sphinx-Needs Compatible**: Follows official needsfile schema
- **Versioned Export**: Supports version tracking
- **Structured Data**: Organized by actors, use cases, and relationships
- **Import Ready**: Can be imported using sphinx-needs needimport directive

### Validation Tools

- **Python Validator**: Validates exported JSON against sphinx-needs format
- **Test Suite**: Sample data generation and validation
- **Error Reporting**: Detailed validation feedback

## Usage

Build, test and install with the `./run` command.

### Export Process

1. Open a use case diagram in Visual Paradigm
2. Click the "Export to Sphinx-Needs" button in the toolbar
3. Select output file location
4. Click "Export" to generate JSON file
5. Import into Sphinx using `needimport` directive

### Sphinx Integration

Add to your `.rst` file:

```rst
.. needimport:: path/to/exported_needs.json
   :version: 1.0
   :id_prefix: vp_
   :tags: imported;visual-paradigm
```

### Validation

Install Python dependencies:
```bash
pip install -r requirements.txt
```

Validate exported JSON files against official sphinx-needs schema:
```bash
python3 scripts/validate_needs.py exported_diagram.json
```

Additional validation options:
```bash
# Use custom schema file
python3 scripts/validate_needs.py --schema custom_schema.json exported_diagram.json

# Strict mode (treat warnings as errors)
python3 scripts/validate_needs.py --strict exported_diagram.json
```

### Configuration

The plugin requires Visual Paradigm 17.2+ and Java 11+.
