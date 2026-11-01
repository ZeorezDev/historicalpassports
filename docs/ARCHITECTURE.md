# Architecture and security model

`HistoricalPassports` registers server/common systems. Only the `Dist.CLIENT`
subscriber loads the screen. Common packets deliver NBT to a client-installed
consumer, so no Minecraft client classes are referenced by dedicated-server code.

`Countries` loads an immutable JSON catalog before deferred registry creation.
Every country uses the same `PassportItem` implementation, with an individual
registry entry, generated model, translation keys and locally packaged texture.

`PassportSavedData` lives in the overworld and contains canonical identities,
officer UUIDs and queued returns. `PassportRecord` separates revocation,
expiration and append-only inspection history. Item NBT contains an ID and
random seal, plus untrusted tooltip hints. `verify` always resolves canonical
holder, nationality, authority, dates and history from the server registry.

`PassportService` keeps short-lived server sessions. Each snapshot includes a
random session nonce; requests include no holder/officer identity, dates or
authoritative record values. The server derives those from the authenticated
player. Request direction and strings are bounded at decode time. Histories are
sent four entries per snapshot. Mutations run on the main server thread.

Before a decision the service checks session ownership, expiry, dimension,
current role and continued access. Desk sessions also check chunk loading and
distance before reading the block entity. Inventory sessions search current
inventory slots for the same verified identity. Successful decisions rotate
the nonce; old packets cannot be replayed. Invalid/expired/revoked documents
cannot be approved; denials may still be recorded. A history limit rejects new
entries instead of deleting existing records.

`InspectionDeskEntity` owns a single escrow stack and depositor UUID. It exposes
no hopper capability or client-editable menu slots. Only the document's holder
may initially submit. Owner/officer/operator returns always route to the
depositor, including on block removal. Full inventories use a persistent mailbox
without dropping items into the world. Repeated returns find empty escrow.

Known limits: ordinary Minecraft world saves are not a transactional database;
hard crashes can roll back recent changes. Creative clones can represent the
same canonical identity. Operators and other server mods remain trusted. No
anti-copy physical serial tracking, automatic border teleport/barrier, visa
issuance or independent external permission-provider integration is included.

GameTests execute actual server services and Forge registries, including
NBT disk round-trip, roles, forged seals, cloned items, escrow and nonce replay.
The session tests use reflection solely to capture the server nonce that a real
client receives. They do not bypass the action validation under test.
