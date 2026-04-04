box.cfg { listen = '0.0.0.0:3301' }

box.once('init', function()
    local kv = box.schema.space.create('KV', {
        format = {
            {name = 'key', type = 'string'},
            {name = 'value', type = 'varbinary', is_nullable = true},
        },
        if_not_exists = true,
    })

    kv:create_index('primary', {
        parts = {'key'},
        type = 'TREE',
        if_not_exists = true,
    })

    box.schema.user.create('testuser', {
        password = 'testpass',
        if_not_exists = true,
    })

    box.schema.user.grant('testuser', 'read,write', 'space', 'KV', {
        if_not_exists = true,
    })

    box.schema.user.grant('testuser', 'execute', 'universe', nil, {
        if_not_exists = true,
    })
end)

require('log').info('Tarantool KV ready')
