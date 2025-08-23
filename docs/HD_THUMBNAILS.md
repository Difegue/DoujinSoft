# HD Thumbnail Generation

This implementation adds high-quality thumbnail generation for .mio game files, porting the functionality from the Python script `tools/mio_hd_thumbnail.py` to Java.

## Features

- **HD Thumbnails**: Generates 192x128px previews (vs 95x63px standard previews)
- **Full Rendering**: Includes background and up to 15 sprite objects with proper positioning
- **Server Integration**: Available through the main Java server without external dependencies

## Usage

### Programmatic API

```java
// Generate standard preview (95x63px)
String preview = MioUtils.getGamePreview(mioBytes, false);

// Generate HD preview (192x128px) with fallback
String hdPreview = MioUtils.getGamePreview(mioBytes, true);

// Direct HD generation (returns null if invalid)
String hdPreview = MioUtils.getHDGamePreview(mioBytes);
```

### HTTP Endpoint

HD previews can be requested via the download servlet:

```
/download?type=game&id=HASH&preview&hd
```

Parameters:
- `type=game`: Request game content
- `id=HASH`: Game hash identifier  
- `preview`: Request preview image instead of .mio file
- `hd`: Generate HD preview (192x128px) instead of standard

## Implementation Details

- **Bit Field Operations**: Uses `BitField` utility class for complex bit manipulation
- **Color Palette**: Implements the same 16-color palette as the Python script
- **Object Positioning**: Handles attachments, area placement, and layering logic
- **Error Handling**: Falls back to standard preview if HD generation fails
- **Performance**: On-demand generation from compressed .mio files

## Compatibility

- Maintains full backward compatibility with existing preview functionality
- Standard previews remain the default to preserve current behavior
- HD generation only triggered when explicitly requested