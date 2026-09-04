# Notebook phone/watch data protocol v1

## Transport

Messages are exchanged using Garmin Connect IQ's application-message transport (messages queued in 
Garmin Connect app). The Android app sends a top-level list containing one dictionary.

Every request dictionary contains:

```text
v: 1
type: "request"
requestId: unique UUID string
operation: operation name
```

The watch responds with:

```text
v: 1
type: "response"
requestId: copied request ID
operation: copied operation name
ok: true or false indicating if the request was successful
error: human-readable string when ok is false
```

Successful data change operations include a full `directories` snapshot. If they do not, Android app immediately issues `list_directories`.

## Operations

- `list_directories`
- `create_directory`: `name`
- `rename_directory`: `directoryId`, `name`
- `delete_directory`: `directoryId`
- `create_text_item`: `directoryId`, `name`, `text`
- `update_text_item`: `directoryId`, `itemId`, `text`
- `create_image_item`: `directoryId`, `name`, `width`, `height`, `palette`, `pixels`, `encoding`
- `rename_item`: `directoryId`, `itemId`, `name`
- `delete_item`: `directoryId`, `itemId`

## Snapshot shape

```text
directories: [
  {
    id: stable string,
    name: string,
    items: [
      { id: string, name: string, type: "text", text: string },
      { id: string, name: string, type: "image", width: number, height: number }
    ]
  }
]
```

Snapshots do not include image files itself.

## Image encoding

Android app scales each selected image so its longest edge is at most 128 pixels. It maps pixels to a fixed 64-color RGB222 palette, and Base64-encodes the bytes.

- `encoding`: `rgb222-index8`
- `palette`: 64 RGB integers in `0xRRGGBB` form
- `pixels`: 8-bit palette indexes in one row

The watch validates dimensions, palette length, decoded length, and storage quota before accepting an image.