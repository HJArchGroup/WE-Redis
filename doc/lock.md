#Usage of Lock-Related Commands#

The lock group contains 3 commands: `TRYLOCK`, `UNLOCK`, and `LOCKSTATUS`. All of these commands should be followed by a `mutex` argument.

A `mutex` is a kind of variable that can be `LOCKED` or `UNLOCKED`. Such a variable cannot be set to any other kind of variables. The command `SET mutex ...` will always return `WRONGTYPE Operation against a key holding the wrong kind of value`. To free a `mutex`, use `DEL mutex`.

##`TRYLOCK mutex`##

Set the status of `mutex` to `LOCKED`.

- If `mutex` does not exist, this command returns `OK`.
- If `mutex` is a string or any other variable kind, this command returns `WRONGTYPE Operation against a key holding the wrong kind of value`.
- If `mutex` has been locked by the current client already, this command returns `ERR lock re-entry`.
- If `mutex` is currently locked by another active (namelly connected) client, this command returns `ERR`.
- If `mutex` is locked by a disconnected client, this command returns `OK`.

##`UNLOCK mutex`##

Set the status of `mutex` to `UNLOCKED`.

- If `mutex` does not exist, this command returns `ERR no such lock`.
- If `mutex` is a string or any other variable kind, this command returns `WRONGTYPE Operation against a key holding the wrong kind of value`.
- If `mutex` is currently locked by the current client, this command returns `OK`.
- If `mutex` is currently locked by another active client, this command returns `ERR`.
- If `mutex` is unlocked, this command returns `OK`.  

##`LOCKSTATUS mutex`##

Return the current status of `mutex`.

- If `mutex` has never been locked by any client ever before, this command returns `ERR no such lock`.
- If `mutex` is a string or any other variable kind, this command returns `WRONGTYPE Operation against a key holding the wrong kind of value`.
- If `mutex` is currently locked by an active client, this command returns `LOCKED`.
- If `mutex` is locked by a disconnected client, or the `mutex` has been successfuly unlocked by a client, this command returns `UNLOCKED`.
