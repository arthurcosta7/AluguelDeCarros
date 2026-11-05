import { useEffect, useState } from 'react'
import { api } from './api'

export function useResource(path, revision = 0) {
  const key = `${path}:${revision}`
  const [state, setState] = useState({ key: null, data: null, error: null })
  useEffect(() => {
    let active = true
    api(path).then(data => { if (active) setState({ key, data, error: null }) })
      .catch(error => { if (active) setState({ key, data: null, error }) })
    return () => { active = false }
  }, [path, key])
  return state.key === key ? { ...state, loading: false } : { data: null, error: null, loading: true }
}

export function useRoute() {
  const [route, setRoute] = useState(() => window.location.hash.slice(1) || '/painel')
  useEffect(() => {
    const change = () => {
      setRoute(window.location.hash.slice(1) || '/painel')
      document.getElementById('content')?.focus()
      window.scrollTo(0, 0)
    }
    window.addEventListener('hashchange', change)
    return () => window.removeEventListener('hashchange', change)
  }, [])
  return route
}
