# Visual Paradigm Sphinx-Needs Plugin

A Visual Paradigm plugin for bidirectional integration with Sphinx-Needs. Import and export use case diagrams in sphinx-needs JSON format with full support for real-world JSON files, relationships, and metadata.

## Architecture

- **Visual Paradigm Plugin API**: Core integration with Visual Paradigm
- **Jackson JSON**: JSON serialization/deserialization
- **Sphinx-Needs Format**: Compatible with sphinx-needs needsfile schema
- **Java Swing**: User interface components

## Features

### Import/Export Capabilities

- **Bidirectional Support**: Both import and export sphinx-needs JSON files
- **Real-world JSON Import**: Handles complete sphinx-needs JSON files with all metadata
- **Actor Import/Export**: Import/export actors as separate needs with "actor" type
- **Use Case Import/Export**: Import/export use cases as separate needs with "uc" type
- **Relationship Handling**: Process Include, Extend, and Associate relationships as arrays
- **Priority Support**: Import priority values and apply to Visual Paradigm use case rankings
- **Model Reuse**: Intelligent reuse of existing Visual Paradigm models during import
- **Metadata Preservation**: Preserve Visual Paradigm source IDs and element types

### JSON Format

- **Sphinx-Needs Compatible**: Follows official needsfile schema v5.1.0
- **Native Array Support**: Uses proper JSON arrays instead of comma-separated strings
- **Unknown Field Handling**: Gracefully ignores sphinx-needs metadata fields not needed for Visual Paradigm
- **Versioned Data**: Supports version tracking and creator information
- **Schema Validation**: Built-in validation against sphinx-needs schema

### Data Model

- **Type-Safe Arrays**: Uses `List<String>` for tags, links, and relationships
- **Consistent Import/Export**: Same data structure for both operations
- **Future-Proof**: Handles new sphinx-needs fields automatically
- **Clean Architecture**: No string manipulation or parsing required

## Usage

Build, test and install with the `./run` command.

### Export Process

1. Open a use case diagram in Visual Paradigm
2. Set User IDs for elements you want to export:
   - Use cases (e.g., "UC001", "UC002")
   - Actors (e.g., "ACT001", "ACT002")
3. Create Include, Extend, or Associate relationships between use cases
4. Create associations between actors and use cases
5. Click the "Export to Sphinx-Needs" button in the toolbar
6. Select output file location
7. Click "Export" to generate JSON file with relationships
8. Import into Sphinx using `needimport` directive

**Note**: Only elements (use cases and actors) with User IDs set will be exported. Relationships between exported elements will be included as JSON arrays.

### Import Process

1. Click the "Import from Sphinx-Needs" button in the toolbar
2. Select a sphinx-needs JSON file (supports real-world files with complete metadata)
3. The plugin will:
   - Parse the JSON file and extract use cases and actors
   - Create or reuse Visual Paradigm models intelligently
   - Apply priority rankings to use cases (high/medium/low)
   - Build Include, Extend, and Associate relationships
   - Position elements in a new use case diagram
4. Review the imported diagram and relationships

**Note**: The plugin handles all sphinx-needs metadata automatically and ignores fields not relevant to Visual Paradigm.

### Sphinx Integration

Add to your `.rst` file:

```rst
.. needimport:: path/to/exported_needs.json
   :version: 1.0
   :id_prefix: vp_
   :tags: imported;visual-paradigm
```

The imported elements will include relationship information:

**Use Cases:**
- **extends**: Links to use cases that this use case extends
- **includes**: Links to use cases that this use case includes
- **associates**: Links to actors or other use cases that this use case associates with

**Actors:**
- **associates**: Links to use cases that this actor associates with

Example imported elements with relationships:
```rst
.. uc:: Login to System
   :id: vp_UC001
   :extends: vp_UC002
   :includes: vp_UC003
   :associates: vp_ACT001
   :tags: imported;visual-paradigm

.. actor:: System User
   :id: vp_ACT001
   :associates: vp_UC001, vp_UC002
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

### Sphinx-Needs Configuration

To enable use case relationship display, add the following to your `conf.py`:

```python
# Enable custom link types for use case relationships
needs_extra_links = [
    {
        "option": "extends",
        "incoming": "is extended by",
        "outgoing": "extends"
    },
    {
        "option": "includes",
        "incoming": "is included by",
        "outgoing": "includes"
    },
    {
        "option": "associates",
        "incoming": "is associated with",
        "outgoing": "associates"
    }
]

# Configure status values for Visual Paradigm use cases
needs_statuses = [
    {"name": "identify", "description": "Use case is in identification phase"},
    {"name": "discuss", "description": "Use case is being discussed"},
    {"name": "elaborate", "description": "Use case is being elaborated"},
    {"name": "design", "description": "Use case is in design phase"},
    {"name": "consent", "description": "Use case has received consent"},
    {"name": "develop", "description": "Use case is being developed"},
    {"name": "complete", "description": "Use case is complete"},
]

# Configure types for Visual Paradigm elements
needs_types = [
    {
        "directive": "uc",
        "title": "Use Case",
        "prefix": "UC",
        "color": "#BFD8D2",
        "style": "node"
    },
    {
        "directive": "actor",
        "title": "Actor",
        "prefix": "ACT",
        "color": "#FEDCD2",
        "style": "actor"
    }
]
```

This configuration enables:
- **Bidirectional relationship display** with custom link types
- **Status tracking** for use case implementation progress
- **Visual styling** for different element types in your documentation

### System Requirements

The plugin requires Visual Paradigm 17.2+ and Java 11+.
